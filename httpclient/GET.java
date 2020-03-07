
import java.net.*;

public class GET extends HTTPRequest {

  // Takes the entire url in the form of http://www.hostName.com/path/to/page?possibeQuery=thisvalue&....
  public static GET make (String urlString) {
    URL url = null;
    try {
      url = new URL(urlString);
      return new GET(url.getHost(), url.getPath() + (url.getQuery() == null ? "" : "?" + url.getQuery()), 8088);
    } catch( Exception e) {
      System.out.println("Failed to create GET request in GET.makeRequest(...)");
      return null;
    }
  }

  private GET(String hostname,  String path, int port) {
    super(hostname, path, port);
  }

  public HTTPResponse send() {
    return super.send(HTTPRequest.RequestType.GET);
  }


}