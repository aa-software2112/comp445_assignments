import java.io.*;
import java.net.*;

public class driver {

  public static void GET() {
    String hostname = "httpbin.org";
    int port = 80;
    String contentTypes = "Content-Length: 4\r\nAllow: GET, POST\r\n\r\n" + "test";
    try {
      String data = URLEncoder.encode("test data found here", "UTF-8");
      String requestLine = "GET /get?d=" + URLEncoder.encode("test data found here", "UTF-8") + " HTTP/1.0\r\n";
      System.out.println("1");
      Socket clientSocket = new Socket(hostname, port);
      PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);
      BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      System.out.println("2");
      //output.println("GET /status/418 HTTP/1.0\r\n;");
      output.println(requestLine + contentTypes);
      System.out.println("3");
      String response = "";
      while((response = input.readLine()) != null) {
        System.out.println(response);
      }
      System.out.println("4");
    } catch(Exception e) {
      System.out.println(e);
      System.exit(1);
    }
  }

  public static void POST() {
    String hostname = "httpbin.org";
    int port = 80;
    //String contentTypes = "Content-Type: text/plain\r\nAllow: GET, POST\r\n\r\n";
    
    try {
      String data = URLEncoder.encode("key1", "UTF-8") + "=" + URLEncoder.encode("value1", "UTF-8");
      String contentTypes = "Content-Type: text/plain\r\nAllow: GET, POST\r\nContent-Length: 4\r\n\r\n" + "test";
      String requestLine = "POST /post HTTP/1.0\r\n";
      System.out.println("1");
      Socket clientSocket = new Socket(hostname, port);
      PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);
      BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      System.out.println("2");
      //output.println("GET /status/418 HTTP/1.0\r\n;");
      output.println(requestLine + contentTypes);
      System.out.println("3");
      String response = "";
      while((response = input.readLine()) != null) {
        System.out.println(response);
      }
      System.out.println("4");
    } catch(Exception e) {
      System.out.println(e);
      System.exit(1);
    }
  }

  public static void main(String[] args) {
    GET();
  }
}