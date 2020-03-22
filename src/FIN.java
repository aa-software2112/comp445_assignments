
public class FIN extends Packet {

  FIN(Integer seq, String peerAddr, Integer port) {
    super(Packet.PACKET_TYPE.FIN, seq, peerAddr, port, "");
  }

  public FIN(byte[] packetData) {
    super(packetData);
  }

  public ACK generateAck() {
    return new ACK(this);
  }
}