import java.io.*;
import java.net.*;

import jdk.nashorn.internal.ir.RuntimeNode.Request;

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
    HTTPClient.setup();
    HTTPClient.processArgs(args);
    //System.out.println(url.getHost());
    //System.out.println(url.getPath());
    //String t = "/search?q=ign&rlz=1C1CHBF_enCA705CA705&oq=ign&aqs=chrome..69i57j0l7.1255j0j8&sourceid=chrome&ie=UTF-8";
    //new HTTPRequest("httpbin.org", 80, "/get")
    //new HTTPRequest(url.getHost(), url.getPath() + (url.getQuery() == null ? "" : "?" + url.getQuery()), 80)
    //.addHeader("Allow", "GET, POST")
    //.addHeader("Content-Type", "text/plain")
    //.
    //.addHeader("Accept", "*/*")
    //.addToBody("!!!!!!This is test body data!!!!!!")
    //.send(HTTPRequest.RequestType.POST);
    //.send(HTTPRequest.RequestType.GET);
    //GET.make("https://www.google.com/search?sxsrf=ACYBGNTNPCRioItamZhJgEOdVMIUknTXLQ%3A1579665819053&source=hp&ei=m8knXtorouvmAvrsnqgD&q=The+document+has+moved+java+client&oq=The+document+has+moved+java+client&gs_l=psy-ab.3..33i160.18134.22410..22497...5.0..0.140.1397.2j10......0....2j1..gws-wiz.....10..35i362i39j0j0i22i30j33i22i29i30._1UOOJ55D70&ved=0ahUKEwia5YXgqZbnAhWitVkKHXq2BzUQ4dUDCAg&uact=5")
    //  .send();
    //POST.make("http://httpbin.org/post")
    //  .addToBody("THIS IS THE POST BODY!")
    //  .send();

  }
}