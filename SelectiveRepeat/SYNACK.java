package SelectiveRepeat;

public class SYNACK extends Packet {

  SYNACK(Integer seq, String peerAddr, Integer port) {
    super(Packet.PACKET_TYPE.SYN_ACK, seq, peerAddr, port, "");
  }

  public SYNACK(byte[] packetData) {
    super(packetData);
  }

  /** This is not a copy constructor - it generates a SYNACK for the passed SYN Packet */
  public SYNACK(SYN toAck) {
    this(toAck.seqNumber, toAck.peerAddress, toAck.port);
  }

  public ACK generateAck() {
    return new ACK(this);
  }
}