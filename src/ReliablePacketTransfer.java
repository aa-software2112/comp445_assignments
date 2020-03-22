import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ReliablePacketTransfer {

  private ARQ arq;
  private Integer peerPort;
  private String peerAddress;
  private static final Integer ROUTER_PORT = 3005;
  private ConcurrentLinkedQueue<String> receiveQueue;

  /** Supports localhost IP */
  ReliablePacketTransfer(UDPSenderReceiver clientServer, Integer peerPort) {
    this(clientServer, peerPort, "localhost");
  }

  ReliablePacketTransfer(UDPSenderReceiver clientServer, Integer peerPort, String ipAddress) {
    this.receiveQueue = new ConcurrentLinkedQueue<String>();
    this.arq = new ARQ(clientServer, ROUTER_PORT);
    this.arq.setExternalQueue(receiveQueue);
    this.arq.start();
    this.peerPort = peerPort;
    this.peerAddress = peerAddress;
  }
  
  public void handshake() {
    this.arq.sendSYN(peerPort, peerAddress);
    this.arq.externalWaitForSynAck();
    System.out.println("Handshake complete!");
  }

  public void sendUniDirectionalMessage(String message) {
    handshake();
    this.arq.sendMessage(this.peerPort, this.peerAddress, message);
    // TODO: teardown
  }

  public void applicationSend(String message) {
    System.out.println("********************A*******************");
    handshake();
    // Send message from client to server
    System.out.println("********************B*******************");
    arq.sendMessage(this.peerPort, this.peerAddress, message);

    // Wait for response
    System.out.println("********************C*******************");
    //listen();

    // Send response UP to client

    // Teardown
    System.out.println("********************D*******************");
    teardown(true);
    System.out.println("Teardown complete!");
  }

  public void listen() {
    while (true) {
      this.arq.externalWaitForMsg();
      int packetsExpected = 0;
      int packetsRead = 0;
      while (!this.receiveQueue.isEmpty()) {
        String message = this.receiveQueue.poll();
        if (message.contains("Content-Length") {
          
        }
        
      }
    }
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

  public void applicationListen() {
    // Wait for data
    System.out.println("********************A*******************");
    listen();
    // Send message UP to application
    // Wait for response from application
    // Send message to client
    // teardown
    System.out.println("********************B*******************");
    teardown(false);
    System.out.println("Teardown complete!");
  }
}