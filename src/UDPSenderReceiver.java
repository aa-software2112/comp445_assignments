import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.IOException;
import java.net.SocketException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.UnknownHostException;

public class UDPSenderReceiver extends Thread {

  private DatagramSocket socket;

  UDPSenderReceiver(Integer port) {
    try {
      socket = new DatagramSocket(null);
      socket.bind(new InetSocketAddress("localhost", port));
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
      DatagramPacket readPacket = new DatagramPacket(this.newBytes(), 1024);
      
      try {
        this.socket.receive(readPacket);
        System.out.printf("Received message FROM: %s PORT: %d\n", readPacket.getAddress(), readPacket.getPort());

        Packet p = PacketFactory.rebuild(readPacket.getData());
        System.out.println(p);
        if (p.isAck()) {
          System.out.println("Acknowledgement!");
        } else {
          System.out.println("Sending ACK");
          Packet a = p.generateAck();
          this.send(readPacket.getPort(), a.getBytes());
        }

      } catch (Exception e) {
        System.out.println(e);
        break;
      }
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

  public byte[] newBytes() {
    return new byte[1024];
  } 

  public void close() {
    this.socket.close();
  }

  public String toString() {
    return String.format("ADDR %s PORT %d", socket.getLocalAddress(), socket.getLocalPort());
  }
}