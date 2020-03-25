package SelectiveRepeat;
import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ARQ extends Thread {
  private enum PACKET_STATE {
    CONSTRUCTING, EN_ROUTE, ACKNOWLEDGED
  } // For OutboundThreads

  private UDPSenderReceiver clientServer;
  private Integer routerPort;
  private static final Integer WINDOW_SIZE = 5;
  // ARQ sender
  private Integer sendingSequenceBase;
  private Integer sendingSequenceNumber;
  private ConcurrentLinkedQueue<Packet> preWindowQueue;
  private CircularArrayList<OutboundPacket> windowList; // For sender

  // ARQ receiver
  private Integer receivingSequenceNumber; // This is the receiver base
  private CircularArrayList<Packet> ackWindowList; // For receiver

  private PacketMonitor packetMonitor;

  // For upper applications to read data
  // Mapping of sequence id -> payload
  private ConcurrentLinkedQueue<Packet> upperQueue;

  // For notifying when a SYNACK was received
  private Object synAckMonitor = new Object();
  private boolean synAckReceived;

  // For notifying when FIN was received
  private Object finMonitor = new Object();
  private boolean finReceived;

  // For notifying ACKS
  private Object ackNotifier = new Object();

  ARQ(UDPSenderReceiver clientServer, Integer routerPort) {
    this.clientServer = clientServer;
    this.routerPort = routerPort;

    // Random starting sequence number between [1, 10]
    // When the handshaking process occurs, the receiver of SYN will
    // take the sequence id and set it as its own. This will mean that
    // initially, both client and server will start with the same sequence numbers
    // for both sending and receiving.
    // dynamic growing [SENDER]
    this.preWindowQueue = new ConcurrentLinkedQueue<Packet>();
    // Will only use WINDOW_SIZE elements, but doubling prevents 
    this.windowList = new CircularArrayList<OutboundPacket>(WINDOW_SIZE * 2);

    // Must clear out actual room for all indexes because messages may
    // be received in any order (of sequence number) [RECEIVER]
    this.ackWindowList = new CircularArrayList<Packet>(WINDOW_SIZE * 2);
    for (int i = 0; i < WINDOW_SIZE * 2; i++) {
      this.ackWindowList.add(null);
    }
    this.reset();
    this.packetMonitor = new PacketMonitor();
    this.packetMonitor.start();

  }
  
  protected void reset() {
    this.synAckReceived = false;
    this.finReceived = false;
    this.sendingSequenceNumber = Integer.MIN_VALUE;//(int) (Math.random() * 10) + 1;
    this.sendingSequenceBase = this.sendingSequenceNumber;
    this.receivingSequenceNumber = Integer.MIN_VALUE;
    for (int i = 0; i < WINDOW_SIZE * 2; i++) {
      this.ackWindowList.set(i, null);
    }
  }

  protected void instanceReset() {
    this.reset();
  }

  public void setExternalQueue(ConcurrentLinkedQueue<Packet> upperQueue) {
    this.upperQueue = upperQueue;
  }

  public void run() {

    while (true) {
      // the preWindowQueue should be notified when
      // A) The preWindowQueue is added-to
      // B) The windowList is removed-from
      synchronized (this.preWindowQueue) {
        if (this.preWindowQueue.isEmpty()) {
          try {
            this.preWindowQueue.wait();
          } catch (Exception e) {
            System.out.println(e);
          }
        }
        synchronized (this.windowList) {
          // Data exists
          while (!this.preWindowQueue.isEmpty() && this.windowList.size() < WINDOW_SIZE) { // Packets -> Thread
            Packet p = this.preWindowQueue.poll();
            OutboundPacket tPacket = new OutboundPacket(p);
            this.windowList.add(tPacket);
            tPacket.start();
          }
        }
      }
    }
  }

  /**
   * Splits a message into packets, and adds each to the pre-window queue
   * @param peerPort: The port the router will use for re-sending the message
   * @param peerAddress: The address the router will use for re-sending the message
   * @param message: The message to be sent
   * @return: Sends back the starting sequence number, to help upper layers look for them later
   */
  synchronized public Integer sendMessage(Integer peerPort, String peerAddress, String message) {
    Packet packets[] = PacketFactory.generatePackets(this.sendingSequenceNumber, peerAddress, peerPort, message);
    return this.addPreWindowPackets(packets);
  }

  synchronized public Integer sendSYN(Integer peerPort, String peerAddress) {
    setInitialConditions(false);
    return this.addPreWindowPacket(new SYN(this.sendingSequenceNumber, peerAddress, peerPort));
  }

  synchronized private Integer sendSYNACK(Integer peerPort, String peerAddress) {
    setSecondaryConditions(false);
    Packet synack = new SYNACK(this.receivingSequenceNumber++, peerAddress, peerPort);
    // This special message includes a payload with the sequence number of this host
    synack.setPayload(this.sendingSequenceBase.toString());
    return this.addPreWindowPacket(synack);
  }

  synchronized public Integer sendFIN(Integer peerPort, String peerAddress) {
    Packet fin = new FIN(this.sendingSequenceNumber, peerAddress, peerPort);
    return this.addPreWindowPacket(fin);
  }
  
  /** 
   * The host sending the SYN should setup the sending sequence number
   */
  private void setInitialConditions(boolean random) {
    if (random) {
      this.sendingSequenceNumber = (int) (Math.random() * 10) + 1;
    } else {
      this.sendingSequenceNumber = 12;
    }
    this.sendingSequenceBase = this.sendingSequenceNumber;
    this.receivingSequenceNumber = Integer.MIN_VALUE;
  }

  /**
   * The host receiving a SYNACK should setup their own sequence numbers 
   */
  private void setSecondaryConditions(boolean random) {
    if (random) {
      this.sendingSequenceNumber = (int) (Math.random() * 10) + 1;
    } else {
      this.sendingSequenceNumber = 15;
    }
    this.sendingSequenceBase = this.sendingSequenceNumber;

  }

  /** Adds a list of packets to the pre-window queue,
   * and returns the sequence number of the expected ACK for the LAST message in list of packets
   * Helpful for preventing next steps of reliable message sequence
   */
  private int addPreWindowPackets(Packet packets[]) {
    int ret = this.sendingSequenceNumber;
    synchronized (this.preWindowQueue) {
      for (Packet p : packets) {
        this.preWindowQueue.add(p);
      }
      this.preWindowQueue.notify();
    }
    this.sendingSequenceNumber += packets.length; // Base sequence number doesn't change yet
    return this.sendingSequenceNumber;
  }

  private int addPreWindowPacket(Packet p) {
    Packet parr[] = { p };
    return this.addPreWindowPackets(parr);
  }

  class OutboundPacket extends Thread {

    private Packet packet;
    private PACKET_STATE packetState;
    private int localMSTimeout;

    OutboundPacket(Packet packet) {
      this.packet = packet;
      this.localMSTimeout = 250; // First timeout is 250 milliseconds
      this.packetState = PACKET_STATE.CONSTRUCTING;
    }

    public void run() {
      while (true) {
        this.packetState = PACKET_STATE.EN_ROUTE;
        System.out.printf("Sending message SEQ[%d] TYPE[%s]\n", packet.getSeqNum(), packet.getType());
        ARQ.this.clientServer.send(ARQ.this.routerPort, this.packet.getBytes());

        synchronized (this) {
          try {
            this.wait(this.localMSTimeout);
          } catch (Exception e) {
            System.out.println(e);
          }

          if (this.isAcknowledged()) {
            break; // We are done with this thread
          } else { /// Double the timeout, send again...
            this.localMSTimeout *= 1.05;
            System.out.printf("Doubling timeout to %d for seq packet %d\n", this.localMSTimeout,
                this.packet.getSeqNum());
          }
        }
      }
    }

    public void acknowledge() {
      // Update and wakeup thread
      this.packetState = PACKET_STATE.ACKNOWLEDGED;
      synchronized (this) {
        this.notify();
      }
    }

    public boolean isAcknowledged() {
      return this.packetState == PACKET_STATE.ACKNOWLEDGED;
    }
  }

  class PacketMonitor extends Thread {
    private ARQ ref;

    PacketMonitor() {
      this.ref = ARQ.this;
    }

    public void run() {
      while (true) {
        DatagramPacket receivedPacket = ref.clientServer.getDatagram();
        if (receivedPacket == null) {
          continue;
        }
        synchronized (ref.windowList) { // this.windowList must be accessed w/ mutual exclusion

          Packet p = PacketFactory.rebuild(receivedPacket.getData());
          System.out.printf("Received message SEQ[%d] TYPE[%s] \n", p.getSeqNum(), p.getType());
          if (p.isAck() || p.isSynAck()) { // The message is an acknowledgement
            int seq = p.getSeqNum();
            if (seq < ref.sendingSequenceBase) { // Already received this ACK! Drop the duplicate ACK
              if (p.isSynAck()) {
                // Must re-acknolwedge a syn-ack as it is also a message needing
                // acknolwedgement.
                System.out.println("Re-ack syn-ack");
                p.setSequenceNumber(Integer.parseInt(p.payload.trim()));
                ref.ackPacket(p);
              }
              System.out.println("Already received ACK for seq#: " + seq);
            } else { // Register the acknowledgement
              int idx = seq - ref.sendingSequenceBase;
              System.out.println("Index of sequence number: " + idx);
              ref.display();
              ref.windowList.get(idx).acknowledge();

              // If the index is 0, dequeue all sequential acknowledged packets
              if (idx == 0) {
                while (ref.windowList.size() > 0 && ref.windowList.get(0).isAcknowledged()) {
                  ref.windowList.remove(0); // Remove from head
                  ref.sendingSequenceBase++;
                  synchronized (this.ref.ackNotifier) {
                    this.ref.ackNotifier.notify();
                  }
                }
              }
              ref.display();

              // A SYN-ACK requires an extra acknolwedgement
              if (p.isSynAck()) {
                Integer rsn = Integer.parseInt(p.payload.trim());
                ref.receivingSequenceNumber = rsn; // ackPacket will do the increment for us.
                p.setPayload("");
                p.setSequenceNumber(rsn);
                System.out.printf("[SYN-ACK.2] Receiving Sequence numbers set to [%d]\n", ref.receivingSequenceNumber);
                System.out.println("Acking syn-ack");
                ref.ackPacket(p);
                ref.synAckReceived = true;
                synchronized (ref.synAckMonitor) {
                  ref.synAckMonitor.notify();
                }
              }
            }
          } else { // The message is not an ACK, so it needs to be sent an ACK!
            /**
             * A SYN must be responded to via a SYN-ACK, which is both an ACK, and
             * a regular outbound message, and so much be added to both windowList,
             * and ackWindowList (done virtually, but not actually).
             * 
             * Since a SYN represents the start of a handshake, store the incoming sequence
             * number as the receivingSequenceNumber, and the starting number as well
             */
            if (p.isSyn()) { // This message requires a SYN ACK, runs as repeat-send thread
              
              if (p.getSeqNum() + 1 == ref.receivingSequenceNumber) {
                System.out.println("Re-received SYN, doing nothing!");
                continue;
              }
              // Set the next expected message from sending-host
              ref.receivingSequenceNumber = p.getSeqNum();
              System.out.printf("[SYN.1] Receiving Sequence number set to [%d]\n", ref.receivingSequenceNumber);

              // Generate sendSequenceNumber & base and send out SYN-ACK
              this.ref.sendSYNACK(p.port, p.peerAddress);
              //this.ref.addPreWindowPacket(p.generateAck());
            } else if (p.isFIN()) { // Send an ACK & a FIN
              System.out.println("************* RECEIVED FIN *****************");
              ref.ackPacket(p);
              synchronized (this.ref.finMonitor) { // Notify that a FIN was received!
                this.ref.finReceived = true;
                this.ref.finMonitor.notify();
              }
            } else { // All other messages require an ACK!
              ref.ackPacket(p);
            }
          }
        }

      }
    }
  }
  
  private void ackPacket(Packet p) {
    int seqNum = p.getSeqNum();
    int base = this.receivingSequenceNumber;
    System.out.printf("Sending ACK for packet with num %d base %d idx %d\n", seqNum, base, seqNum - base);
    //this.display();
    if (seqNum >= base && (seqNum <= (base + WINDOW_SIZE - 1))) { // Send ACK
      System.out.println("Sending out new ACK...");
      // Store the packet, send the ack
      int idx = seqNum - base;
      this.ackWindowList.set(idx, p);
      this.clientServer.send(this.routerPort, p.generateAck().getBytes());
      // Clear out messages at front of list, passing the responses up to map if it exists
      while (this.ackWindowList.get(0) != null) {
        Packet pd = this.ackWindowList.remove(0);
        if (pd.isData()) {
          synchronized (this.upperQueue) {
            this.upperQueue.add(pd);
            this.upperQueue.notify();
          }
        }
        this.ackWindowList.add(null); // Create blank space at end
        this.receivingSequenceNumber++;
      }
    } else if (seqNum >= (base - ARQ.WINDOW_SIZE) && seqNum <= (base - 1)) { // Send ACK (but dont store packet) - original ACK dropped/timeouted
      System.out.println("Sending out old ACK...");
      Packet ack = p.generateAck();
      this.clientServer.send(this.routerPort, ack.getBytes());
    }
    //this.display();
  }

  private void display() {
    System.out.println("Packets in window: ");
    for (int i = 0; i < this.windowList.size(); i++) {
      System.out.println(this.windowList.get(i).packet.getSeqNum());
    }

    System.out.println("Packets in ack window: ");
    for (int i = 0; i < this.ackWindowList.size(); i++) {
      Packet p = this.ackWindowList.get(i);
      if (p == null) {
        continue;
      }
      System.out.println(p.getSeqNum());
    }
  }

  public void externalWaitForMsg() {
    synchronized (this.upperQueue) {
      while (true) {
        if (this.upperQueue.size() > 0) {
          return;
        }
        try {
          this.upperQueue.wait();
        } catch (Exception e) {
          System.out.println(e);
        }
      }
    }
  }

  public void externalWaitForSynAck() {
    synchronized (this.synAckMonitor) {
      if (this.synAckReceived) {
        return;
      }
      try {
        this.synAckMonitor.wait();
      } catch (Exception e) {
        System.out.println(e);
      }
    }
  }
  
  public void waitFIN(long timeout) {
    synchronized (this.finMonitor) {
      if (this.finReceived) {
        return;
      }
      try {
         // We don't care to check when this monitor is notified again, just return
        if (timeout == -1) {
          this.finMonitor.wait();
        } else {
          this.finMonitor.wait(timeout);
        }
      } catch (Exception e) {
        System.out.println(e);
      }
    }
  }

  public void waitACK(Integer ackNum, long timeout) {
    synchronized (this.ackNotifier) {
      while (true) {
        if (ackNum <= this.sendingSequenceBase) {
          return;
        }
        try {
          if (timeout == -1) {
            this.ackNotifier.wait();
          } else { // We don't care to keep checking if a timeout is provided!
            this.ackNotifier.wait(timeout);
            break;
          }
        } catch (Exception e) {
          System.out.println(e);
        }
      }
    }
  }
}