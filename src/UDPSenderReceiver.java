import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

public class UDPSenderReceiver extends Thread {

  private DatagramSocket socket;
  private LinkedBlockingQueue<DatagramPacket> datagramsReceived;

  UDPSenderReceiver(Integer port) {
    try {
      socket = new DatagramSocket(null);
      socket.bind(new InetSocketAddress("localhost", port));
      this.datagramsReceived = new LinkedBlockingQueue<DatagramPacket>();
    } catch (SocketException e) {
      System.out.println("Error in UDPSenderReceiver constructor... " + e);
      System.exit(1);
    }
  }

  UDPSenderReceiver() { // Ephemeral port
    this(0);
  }

  /**
   * Runs the listener for the Sender/Receiver
   */
  public void run() {

    while (true) {
      DatagramPacket readPacket = new DatagramPacket(this.newBytes(), Packet.MAX_PAYLOAD_SIZE);

      try {
        this.socket.receive(readPacket);
        this.store(readPacket);
        /* System.out.printf("Received message FROM: %s PORT: %d\n", readPacket.getAddress(), readPacket.getPort());

        Packet p = PacketFactory.rebuild(readPacket.getData());
        System.out.println(p); 
        if (p.isAck()) {
          System.out.println("Acknowledgement!");
        } else {
          System.out.println("Sending ACK");
          Packet a = p.generateAck();
          this.send(readPacket.getPort(), a.getBytes());
        }
          */

      } catch (Exception e) {
        System.out.println(e);
        break;
      }
    }
  }
  
  private void store(DatagramPacket receivedDatagram) {
    try {
      this.datagramsReceived.put(receivedDatagram);
    } catch(InterruptedException e) {
      System.out.println("Failed to add packed to queue in .store()... " + e);
    }
  }

  /**
   * Localhost sending only! No need for IP in parameter
   */
  public void send(Integer port, String msg) {
    this.send(port, msg.getBytes());
  }
  
  public void send(Integer port, byte[] msg) {
    try {
      DatagramPacket packet = new DatagramPacket(msg, msg.length, InetAddress.getByName("localhost"), port);
      this.socket.send(packet);
    } catch (UnknownHostException u) {
      System.out.println(u);
    } catch (IOException e) {
      System.out.println(e);
    }
  }

  public DatagramPacket getDatagram() {
    try {
      return this.datagramsReceived.take();
    } catch (InterruptedException i) {
      System.out.println("Error getting datagram in .getDatagram()... " + i);
      return null;
    }
  }


  private byte[] newBytes() {
    return new byte[Packet.MAX_PAYLOAD_SIZE];
  } 

  public void close() {
    this.socket.close();
  }

  public String toString() {
    return String.format("ADDR %s PORT %d", socket.getLocalAddress(), socket.getLocalPort());
  }
}