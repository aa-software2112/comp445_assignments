
public class FIN extends Packet {

  FIN(Integer seq, String peerAddr, Integer port, String payload) {
    super(Packet.PACKET_TYPE.DATA, seq, peerAddr, port, payload);
  }

  public FIN(byte[] packetData) {
    super(packetData);
  }

  public ACK generateAck() {
    return new ACK(this);
  }
}