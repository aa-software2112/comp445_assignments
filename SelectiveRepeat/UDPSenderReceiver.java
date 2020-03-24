package SelectiveRepeat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class UDPSenderReceiver extends Thread {

  private DatagramSocket socket;
  private ConcurrentLinkedQueue<DatagramPacket> datagramsReceived;

  UDPSenderReceiver(Integer port) {
    try {
      socket = new DatagramSocket(null);
      socket.bind(new InetSocketAddress("localhost", port));
      this.datagramsReceived = new ConcurrentLinkedQueue<DatagramPacket>();
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
      byte newBuff[] = this.newBytes();
      DatagramPacket readPacket = new DatagramPacket(newBuff, newBuff.length);

      try {
        this.socket.receive(readPacket);
        this.store(readPacket);
      } catch (Exception e) {
        System.out.println(e);
        break;
      }
    }
  }
  
  private void store(DatagramPacket receivedDatagram) {
    synchronized (this.datagramsReceived) {
      this.datagramsReceived.add(receivedDatagram);
      this.datagramsReceived.notify();
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
      synchronized (this.datagramsReceived) {
        while (this.datagramsReceived.isEmpty()) {
          this.datagramsReceived.wait();
        }
      }
      return this.datagramsReceived.poll();
    } catch (InterruptedException i) {
      System.out.println("Error getting datagram in .getDatagram()... " + i);
      return null;
    }
  }


  private byte[] newBytes() {
    return new byte[Packet.MAX_PACKET_SIZE];
  } 

  public void close() {
    this.socket.close();
  }

  public String toString() {
    return String.format("ADDR %s PORT %d", socket.getLocalAddress(), socket.getLocalPort());
  }
}