
public class SYN extends Packet {

  SYN(Integer seq, String peerAddr, Integer port) {
    super(Packet.PACKET_TYPE.SYN, seq, peerAddr, port, "");
  }

  public SYN(byte[] packetData) {
    super(packetData);
  }

  public SYNACK generateAck() {
    return new SYNACK(this);
  }
}