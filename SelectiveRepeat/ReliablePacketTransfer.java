package SelectiveRepeat;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ReliablePacketTransfer {

  private ARQ arq;
  private Integer peerPort;
  private String peerAddress;
  private static final Integer ROUTER_PORT = 3005;
  private ConcurrentLinkedQueue<String> receiveQueue;

  // Application receives messages through this queue
  private ConcurrentLinkedQueue<String> applicationEar;

  // Application sends messages through this queue
  private ConcurrentLinkedQueue<String> applicationMouth;

  /** Supports localhost IP */
  ReliablePacketTransfer(UDPSenderReceiver clientServer, Integer peerPort) {
    this(clientServer, peerPort, "localhost");
  }

  ReliablePacketTransfer(UDPSenderReceiver clientServer, Integer peerPort, String ipAddress) {
    this.receiveQueue = new ConcurrentLinkedQueue<String>();
    this.applicationEar = new ConcurrentLinkedQueue<String>();
    this.applicationMouth = new ConcurrentLinkedQueue<String>();
    this.arq = new ARQ(clientServer, ROUTER_PORT);
    this.arq.setExternalQueue(receiveQueue);
    this.arq.start();
    this.peerPort = peerPort;
    this.peerAddress = ipAddress;
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

    // Send message from client to server
    System.out.println("********************B*******************");
    arq.sendMessage(this.peerPort, this.peerAddress, message);

    // Wait for response
    System.out.println("********************C*******************");
    listen();

    // Send response UP to client - the client should be calling
    // applicationWaitForMsg(). Regardless, teardown!
    //String response = applicationWaitForMsg();
    
    // Teardown
    System.out.println("********************D*******************");
    teardown(true);
    System.out.println("Teardown complete!");
  }

  private void teardown(boolean asClient) {
    if (!asClient) {
      System.out.println("********************T1*******************");
      int ackNum = this.arq.sendFIN(peerPort, peerAddress); // Send client FIN message
      System.out.println("********************T2*******************");
      this.arq.waitACK(ackNum, -1);
      System.out.println("********************T3*******************");
      this.arq.waitFIN(1000); // Wait 1 second for FIN from client, otherwise just quit
     System.out.println("********************T4*******************");
   } else {
      System.out.println("********************T1*******************");
      this.arq.waitFIN(-1); // Wait infinitely for FIN from server
      System.out.println("********************T2*******************");
      int ackNum = this.arq.sendFIN(peerPort, peerAddress); // Send FIN back to server
      System.out.println("********************T3*******************");
      this.arq.waitACK(ackNum, 1000); // Wait 1 second for ACK, otherwise just quit
      System.out.println("********************T4*******************");
    }
  }

  private void applicationListen() {
    // Wait for data
    System.out.println("********************A*******************");

    // This is a HTTP listen, it waits for a message and assembles it,
    // sending it up to the "ear" or application above when it has been fully combined
    // This function returns once this has been done
    listen();

    System.out.println("********************A.1*******************");
    // Wait for response from application
    String response = waitOnApplicationResponse();

    System.out.printf("********************A.2*******************\n%s", response);
    // Send message to client
    this.arq.sendMessage(peerPort, peerAddress, response);

    // teardown
    System.out.println("********************B*******************");
    teardown(false);
    System.out.println("Teardown complete!");
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

  public void listen() {
    System.out.println("listen1");
    Pattern cntLenPat = Pattern.compile("Content-Length:\\s{0,1}(\\d+)", Pattern.CASE_INSENSITIVE);
    StringBuilder outBuff = new StringBuilder();
    int pkti = 0;
    int PAYLOAD_SIZE = Packet.MAX_PAYLOAD_SIZE;

    while (true) {
      this.arq.externalWaitForMsg();
      String packet = this.receiveQueue.poll();
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
              packet = this.receiveQueue.poll();
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
    synchronized(this.applicationEar) {
      this.applicationEar.add(outBuff.toString());
      this.applicationEar.notify();
    }
  }

}