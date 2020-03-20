
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

  public static Packet[] generatePackets(Integer seq, String peerAddr, Integer port, String payload) {
    int numPackets = (int) Math.ceil(payload.length() / ((float) Packet.MAX_PAYLOAD_SIZE));
    Packet packets[] = new Packet[numPackets];
    for (int i = 0; i < numPackets - 1; i++) {
      packets[i] = new DATA(seq, peerAddr, port,
          payload.substring(i * Packet.MAX_PAYLOAD_SIZE, (i + 1) * Packet.MAX_PAYLOAD_SIZE));
    }
    packets[numPackets - 1] = new DATA(seq, peerAddr, port,
        payload.substring((numPackets - 1) * Packet.MAX_PAYLOAD_SIZE));
    
    return packets;
  }
}