package SelectiveRepeat;

public class RoutePacketHandler implements PacketHandler {
  
  private HashMap<String, ARQ> clientMapping = new HashMap<String, ARQ>();

  public void handlePacket(ARQ ref, Packet p) {
    // Find out where the packet came from
    String clientAddress = p.getAddress();
    Integer clientPort = p.getPort();

    String clientKey = getClientKey(clientAddress, clientPort);
    System.out.printf("Client packet received ADDR: %s PORT: %d KEY: %s\n", clientAddress, clientPort, clientKey);
    if (clientMapping.contains(clientKey)) { // ??

    } else { // ARQ does not exist for this client yet, set it up.
      UDPSenderReceiver newServer = new UDPSenderReceiver();
      newServer.start();
      clientMapping.put(clientKey, new ARQ(newServer, ref.routerPort, new ReliablePacketHandler()));

    }


  }
  
  private String getClientKey(String clientAddress, Integer clientPort) {
    return String.format("%s:%d", clientAddress, clientPort);
  }
}