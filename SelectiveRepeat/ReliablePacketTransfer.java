package SelectiveRepeat;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ReliablePacketTransfer {

  private ARQ arq;
  private Integer peerPort;
  private String peerAddress;
  private static final Integer ROUTER_PORT = 3005;
  private ConcurrentLinkedQueue<Packet> receiveQueue;

  // Application receives messages through this queue
  private ConcurrentLinkedQueue<String> applicationEar;

  // Application sends messages through this queue
  private ConcurrentLinkedQueue<String> applicationMouth;

  // UDP
  private UDPSenderReceiver clientServer;

  /** Used for server who does not know what the peer address is initially
   * We only know it will come over localhost
   */
  public ReliablePacketTransfer(UDPSenderReceiver clientServer) {
    this(clientServer, -1, "localhost");
  }

  /** Supports localhost IP */
  public ReliablePacketTransfer(UDPSenderReceiver clientServer, Integer peerPort) {
    this(clientServer, peerPort, "localhost");
  }

  public ReliablePacketTransfer(UDPSenderReceiver clientServer, Integer peerPort, String ipAddress) {
    this.receiveQueue = new ConcurrentLinkedQueue<Packet>();
    this.applicationEar = new ConcurrentLinkedQueue<String>();
    this.applicationMouth = new ConcurrentLinkedQueue<String>();
    this.arq = new ARQ(clientServer, ROUTER_PORT, new ReliablePacketHandler());
    this.arq.setExternalQueue(receiveQueue);
    this.arq.start();
    this.peerPort = peerPort;
    this.peerAddress = ipAddress;
    this.clientServer = clientServer;
  }

  public void handshake() {
    this.arq.sendSYN(peerPort, peerAddress);
    this.arq.externalWaitForSynAck();
    System.out.println("Handshake complete!");
  }

  public void sendMessage(String message) {
    (new ComInstance(true, message)).start();
  }

  public void start() {
    (new ComInstance(false)).start();
  }

  public class ComInstance extends Thread {

    private boolean asClient;
    private String message;

    ComInstance(boolean asClient, String message) {
      this.asClient = asClient;
      this.message = message;
    }

    ComInstance(boolean asClient) {
      this(asClient, null);
    }

    public void run() {
      if (asClient) {
        ReliablePacketTransfer.this.applicationSend(this.message);
      } else {
        ReliablePacketTransfer.this.applicationListen();
      }
    }
  }
  
  private void applicationSend(String message) {
    System.out.println("********************A*******************");
    handshake();
    arq.toDataState();
    // Send message from client to server
    System.out.println("********************B*******************");
    int waitAck = arq.sendMessage(peerPort, peerAddress, message);
    arq.waitACK(waitAck, -1);
    
    // Wait for response
    System.out.println("********************C*******************");
    listen();

    // Send response UP to client - the client should be calling
    arq.toTeardownState();
    System.out.println("********************D*******************");
    teardown(true);
    System.out.println("Teardown complete!");
    // Close the actual port
    this.arq.close();
  }

  private void teardown(boolean asClient) {
    // Client initiates the teardown
    this.arq.startGlobalTimer();
    boolean done = false;
    if (!asClient) {
      // Wait FIN
      // Send ACK (automatically done)
      // Send FIN
      // Wait ACK
      // Close
      System.out.println("********************T0*******************");
      done = this.arq.waitFIN(-1);
      if (done) {
        // Premature end
        System.out.println("T0 done");
        return;
      }
      System.out.println("********************T1*******************");
      int ackNum = this.arq.sendFIN(peerPort, peerAddress); // Send client FIN message
      System.out.printf("********************T2 %d*******************\n", ackNum);
      done = this.arq.waitACK(ackNum, -1);
      if (done) {
        // Premature end
        System.out.println("T2 done");
        return;
      }
      System.out.println("********************T3*******************");
    } else {
      // Send FIN
      // Wait for ACK
      // Wait for FIN
      // Send ACK (automatically done)
      // Close
      System.out.println("********************T0******************");
      int ackNum = this.arq.sendFIN(peerPort, peerAddress);
      System.out.println("********************T1*******************");
      done = this.arq.waitACK(ackNum, -1);
      if (done) {
        // Premature end
        System.out.println("T1 done");
        return;
      }
      System.out.println("********************T2*******************");
      done = this.arq.waitFIN(-1);
      if (done) {
        // Premature end
        System.out.println("T2 done");
        return;
      }
      System.out.println("********************T4*******************");

    }
  }

  private synchronized void applicationListen() {
    this.arq.acceptMessage();
    System.out.println("\tWaiting for syn");
    this.arq.externalWaitForSyn();
    int port = this.arq.getPeerPortFromSyn();
    // Generate sendSequenceNumber & base and send out SYN-ACK
    int ack = this.arq.sendSYNACK(port, peerAddress);

    System.out.println("\tSending SYNACK");
    System.out.println("\tWaiting for ACK " + ack);
    this.arq.waitACK(ack, -1);
    System.out.println("\tReceived ACK");
    this.arq.toDataState();

    // Wait for data
    System.out.println("********************A*******************");

    // This is a HTTP listen, it waits for a message and assembles it,
    // sending it up to the "ear" or application above when it has been fully combined
    // This function returns once this has been done
    int lpeerPort = listen();

    System.out.println("********************A.1*******************");
    // Wait for response from application
    String response = waitOnApplicationResponse();

    System.out.printf("********************A.2*******************\n%s", response);
    // Send message to client - wait infinitely for last ACK
    int lastAckBeforeTeardown = arq.sendMessage(lpeerPort, peerAddress, response);
    System.out.println("Waiting for last ACK: " + lastAckBeforeTeardown);
    arq.waitACK(lastAckBeforeTeardown, -1);
    
    this.arq.toTeardownState();
    // teardown
    System.out.println("********************B*******************");
    // important to set the peerport before call to teardown @ server
    this.peerPort = lpeerPort;
    teardown(false);
    System.out.println("Teardown complete!");
    this.arq.close();
    this.arq.reset();
    System.out.println("After reset");
  }

  public String applicationWaitForMsg() {
    if (this.applicationEar == null) {
      throw new RuntimeException("Must set application ear before calling this method!");
    }
    synchronized (this.applicationEar) {
      while (true) {
        if (!this.applicationEar.isEmpty()) {
          return this.applicationEar.poll();
        }
        try {
          this.applicationEar.wait();
        } catch (Exception e) {
          System.out.println(e);
        }
      }
    }
  }

  private String waitOnApplicationResponse() {
    synchronized (this.applicationMouth) {
      while (true) {
        if (!this.applicationMouth.isEmpty()) {
          return this.applicationMouth.poll();
        }
        try {
          this.applicationMouth.wait();
        } catch (Exception e) {
          System.out.println(e);
        }
      }
    }
  }

  public void applicationRespond(String response) {
    synchronized (this.applicationMouth) {
      this.applicationMouth.add(response);
      this.applicationMouth.notify();
    }
  }

  /**
   * Listens for data, returns the PORT (localhost) over which
   * the data was received. Especially useful to the server that does not
   * know the port it will receive from ahead of time.
   */
  public int listen() {
    System.out.println("listen1");
    Pattern cntLenPat = Pattern.compile("Content-Length:\\s{0,1}(\\d+)", Pattern.CASE_INSENSITIVE);
    StringBuilder outBuff = new StringBuilder();
    int pkti = 0;
    int PAYLOAD_SIZE = Packet.MAX_PAYLOAD_SIZE;
    int portReceived = -1;
    while (true) {
      this.arq.externalWaitForMsg();
      Packet packetObj = this.receiveQueue.poll();
      String packet = packetObj.getPayload();
      portReceived = packetObj.getPort();
      System.out.println(packet);
      Matcher matches = cntLenPat.matcher(packet); // Attempt to find content-length

      int packetsExpected = 0;
      int packetsRead = 0;
      if (matches.find()) { // Content-length exists!
        int idxBody = -1;
        int contentLength = Integer.parseInt(matches.group(1));

        // Look for body separator
        if ((idxBody = packet.indexOf("\r\n\r\n", 0)) != -1) {
          idxBody += 4; // The start of the body
          if (contentLength + idxBody <= PAYLOAD_SIZE) { // Single packet of body-data
            outBuff.append(packet.substring(0, idxBody + contentLength));
            break; // Done
          } else { // Multiple packets of body-data
            // Handle initial packet separately
            System.out.println("Content-length: " + contentLength);
            outBuff.append(packet.substring(0, PAYLOAD_SIZE));
            contentLength -= PAYLOAD_SIZE - idxBody;
            System.out.println("Content-length: " + contentLength);

            while (contentLength != 0) { // Keep reading until all packets received
              this.arq.externalWaitForMsg(); // Get next packet
              packetObj = this.receiveQueue.poll();
              packet = packetObj.getPayload();
              System.out.println("New packet with length " + packet.length() + " content length " + contentLength);
              if (contentLength < PAYLOAD_SIZE) { // Last packet
                outBuff.append(packet.substring(0, contentLength));
                contentLength -= contentLength;
              } else { // middle packet
                outBuff.append(packet.substring(0, PAYLOAD_SIZE));
                contentLength -= PAYLOAD_SIZE;
              }
            }
            break;
          }
        } else {
          System.out.println("Invalid message... no body found");
          break;
        }
      } else { // Content-length does not exist! Assumes single-packet message
        System.out.println("3");
        outBuff.append(packet);
        break;
      }
    }

    System.out.println("Putting packet in application ear...");
    synchronized (this.applicationEar) {
      this.applicationEar.add(outBuff.toString());
      this.applicationEar.notify();
    }
    return portReceived;
  }

}