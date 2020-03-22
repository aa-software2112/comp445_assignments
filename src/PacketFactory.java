
public class PacketFactory {

  public static Packet rebuild(byte[] packet) {
    int packetType = packet[0];
    Packet ret = null;
    switch (packetType) {
      case 0:
        ret = new DATA(packet);
        break;
      case 1:
        ret = new ACK(packet);
        break;
      case 2:
        ret = new SYN(packet);
        break;
      case 3:
        ret = new SYNACK(packet);
        break;
      case 4:
        ret = new FIN(packet);
        break;
    }
    return ret;
  }

  public static Packet[] generatePackets(Integer seq, String peerAddr, Integer port, String payload) {
    int numPackets = (int) Math.ceil(payload.length() / ((float) Packet.MAX_PAYLOAD_SIZE));
    Packet packets[] = new Packet[numPackets];
    for (int i = 0; i < numPackets - 1; i++) {
      packets[i] = new DATA(seq++, peerAddr, port,
          payload.substring(i * Packet.MAX_PAYLOAD_SIZE, (i + 1) * Packet.MAX_PAYLOAD_SIZE));
    }
    packets[numPackets - 1] = new DATA(seq, peerAddr, port,
        payload.substring((numPackets - 1) * Packet.MAX_PAYLOAD_SIZE));
    
    return packets;
  }
}