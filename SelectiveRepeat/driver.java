package SelectiveRepeat;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class driver {
  
  static String test = 
  "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";

  public void testfn() {
    Packet packet = new DATA(3000, "localhost", 9000, "This is the payload message...");
    UDPSenderReceiver client = new UDPSenderReceiver();
    UDPSenderReceiver server = new UDPSenderReceiver(9000);
    client.start();
    server.start();
    client.send(3005, packet.getBytes());
    try {
      client.join();
      server.join();
    } catch (Exception e) {
      client.close();
      server.close();
    }

  }

  public static String getTestData(String filename) {
    try {
      byte[] f = Files.readAllBytes(Paths.get("./SelectiveRepeat/sampleRequest.txt"));
      return new String(f);
    } catch (Exception e) {
      System.out.println(e);
      return "";
    }
  }
  
  public static void runCS(String args[]) {
    System.out.println(args[0]);
    int serverPort = 9000; // Server on 9000
    int clientPort = 8999; // Client on 8999
    if (args[0].equals("client")) {
      UDPSenderReceiver udpClient = new UDPSenderReceiver(clientPort);
      udpClient.start();
      ReliablePacketTransfer rtf = new ReliablePacketTransfer(
          udpClient, serverPort);
      test = getTestData("sampleRequest.txt");
      rtf.sendMessage(test);
      String clientResponse = rtf.applicationWaitForMsg();
      System.out.println("Client received server's resposne... " + clientResponse);
      //rtf.handshake();
      try{
        udpClient.join();
      } catch(Exception e) {
        System.out.println(e);
      }
    } else if (args[0].equals("server")) {
      System.out.println("here");
      UDPSenderReceiver udpServer = new UDPSenderReceiver(serverPort);
      udpServer.start();
      ReliablePacketTransfer srtf = new ReliablePacketTransfer(
        udpServer, clientPort
      );
      srtf.start();
      String request = srtf.applicationWaitForMsg();
      // Process request here ... 
      srtf.applicationRespond(request);

      System.out.println("Driver message received " + request);
       try{
        udpServer.join();
      } catch(Exception e) {
        System.out.println(e);
      }
    }
  }

  public static void parser() {
    String data = getTestData("sampleRequest.txt");
    Queue<String> incoming = new ConcurrentLinkedQueue<String>();
    //System.out.println("Total data length = " + data.length());
    int numPackets = (int) Math.ceil(((double) data.length()) / 1013);
    //System.out.println(numPackets);
    int i = 0;
    while (numPackets > 0) {
      int diff = data.length() - 1013 * (i);
      int upperBound = diff > 1013 ? 1013 * (i + 1) : 1013*(i) + diff;
      //System.out.printf("lower %d diff %d upper %d\n", (1013*i), diff, upperBound);
      incoming.add(data.substring(1013 * i, upperBound));
      i++;
      numPackets--;
    }

    // Starting from here is the code for procesing packets as they arrive
    Pattern cntLenPat = Pattern.compile("Content-Length:\\s{0,1}(\\d+)", Pattern.CASE_INSENSITIVE);
    StringBuilder outBuff = new StringBuilder();
    int pkti = 0;
    int PAYLOAD_SIZE = 1013;
    while (true) {
      String packet = incoming.poll();
      System.out.println(packet);
      Matcher matches = cntLenPat.matcher(packet);
      System.out.println("1");
      System.out.println(matches);
      if (matches.find()) { // Content-length exists!
        System.out.println("2");
        System.out.println(matches.group(0));
        int contentLength = Integer.parseInt(matches.group(1));

        // Look for body separator
        int idxBody = -1;
        if ((idxBody = packet.indexOf("\r\n\r\n", 0)) != -1) {
          idxBody += 4; // The start of the body

          if (contentLength + idxBody <= PAYLOAD_SIZE) { // Single packet of body-data
            outBuff.append(packet.substring(0, idxBody + contentLength));
            break; // Done
          } else { // Multiple packets of body-data
            // Handle initial packet separately
            System.out.println("Content-length: " + contentLength);
            outBuff.append(packet.substring(0, PAYLOAD_SIZE));
            contentLength -= PAYLOAD_SIZE - idxBody;
            System.out.println("Content-length: " + contentLength);

            while (contentLength != 0) { // Keep reading until all packets received
              packet = incoming.poll(); // Get next packet
              System.out.println("New packet with length " + packet.length() + " content length " + contentLength);
              if (contentLength < PAYLOAD_SIZE) { // Last packet
                outBuff.append(packet.substring(0, contentLength));
                contentLength -= contentLength;
              } else { // middle packet
                outBuff.append(packet.substring(0, PAYLOAD_SIZE));
                contentLength -= PAYLOAD_SIZE;
              }
            }
            break;
          }
        } else {
          System.out.println("Invalid message... no body found");
          break;
        }
      } else { // Content-length does not exist! Assumes single-packet message
        System.out.println("3");
        outBuff.append(packet);
        break;
      }
    }
    
    System.out.println(outBuff.toString());


  }
  public static void main(String[] args) {
    runCS(args);
    
  }
}