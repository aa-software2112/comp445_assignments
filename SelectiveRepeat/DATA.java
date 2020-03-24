package SelectiveRepeat;

public class DATA extends Packet {

  DATA(Integer seq, String peerAddr, Integer port, String payload) {
    super(Packet.PACKET_TYPE.DATA, seq, peerAddr, port, payload);
  }
  
  public DATA(byte[] packetData) {
    super(packetData);
  }

  public ACK generateAck() {
    return new ACK(this);
  }

}