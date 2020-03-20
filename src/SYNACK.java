
public class SYNACK extends Packet {

  SYNACK(Integer seq, String peerAddr, Integer port) {
    super(Packet.PACKET_TYPE.SYN_ACK, seq, peerAddr, port, "");
  }

  public SYNACK(byte[] packetData) {
    super(packetData);
  }
  
  public ACK generateAck() {
    return new ACK(this);
  }
}