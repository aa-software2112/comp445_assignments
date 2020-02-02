import java.net.*;

public class POST extends HTTPRequest {

  // Takes the entire url in the form of http://www.hostName.com/path/to/page
  public static POST make (String urlString) {
    URL url = null;
    try {
      url = new URL(urlString);
      return new POST(url.getHost(), url.getPath() + (url.getQuery() == null ? "" : "?" + url.getQuery()), 80);
    } catch( Exception e) {
      System.out.println("Failed to create POST request in POST.make(...)");
      return null;
    }
  }

  private POST(String hostname,  String path, int port) {
    super(hostname, path, port);
  }

  public POST addToBody(String content) {
    return (POST)super.addToBody(content);
  }

  public HTTPResponse send() {
    return super.send(HTTPRequest.RequestType.POST);
  }


}