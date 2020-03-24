package SelectiveRepeat;
public class ACK extends Packet {

  ACK(Integer seq, String peerAddr, Integer port) {
    super(Packet.PACKET_TYPE.ACK, seq, peerAddr, port, "");
  }

  public ACK(byte[] packetData) {
    super(packetData);
  }

  /** This is not a copy constructor - it generates an ACK for the passed Packet */
  ACK(Packet toAck) {
    this(toAck.seqNumber, toAck.peerAddress, toAck.port);
  }

  public ACK generateAck() {
    return this;
  }
}