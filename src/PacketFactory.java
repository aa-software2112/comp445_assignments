
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
}