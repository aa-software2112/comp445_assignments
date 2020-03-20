
public class SYN extends Packet {

  SYN(Integer seq, String peerAddr, Integer port) {
    super(Packet.PACKET_TYPE.SYN, seq, peerAddr, port, "");
  }

  public SYN(byte[] packetData) {
    super(packetData);
  }

  public ACK generateAck() {
    return new ACK(this);
  }
}