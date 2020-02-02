import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.StringBuilder;

class HTTPRequest {
  private String hostName;
  private Integer port;
  private StringBuilder headers;
  private StringBuilder paramString;
  private StringBuilder body;
  private Socket communicationSocket;
  private PrintWriter outputWriter;
  private BufferedReader inputReader;
  private String path;
  private boolean verbose;
  private int allowedRedirects;
  private static final String LSEP = "\r\n";  
  private static final String HTTP_VERSION = "HTTP/1.0";  
  private static final String STATUS_REGEX = "HTTP\\/1.[0-1] (301|302) FOUND";
  private static final String LOCATION_REGEX = "Location: ([^\\s]*)";

  public enum RequestType {
    GET ("GET"),
    POST ("POST");

    private String action;

    RequestType(String action) {
      this.action = action;
    }

    public String type() {
      return this.action;
    }
  }

  HTTPRequest(String hostName, String path, Integer port) {
    this.hostName = hostName;
    this.port = port;
    this.path = path.length() == 0 ? "/" : path;
    this.headers = new StringBuilder();
    this.paramString = new StringBuilder();
    this.body = new StringBuilder();
    this.verbose = false;
    this.allowedRedirects = 1;
    try {
      this.communicationSocket = new Socket(this.hostName, this.port);
      this.outputWriter =  
        new PrintWriter(this.communicationSocket.getOutputStream(), true);
      this.inputReader = 
        new BufferedReader(new InputStreamReader(this.communicationSocket.getInputStream()));
    } catch (Exception e) {
      System.out.println("Error in Request constructor..." + e);
      System.exit(0);
    }
  }

  public HTTPRequest setRedirects(int redirects) {
    this.allowedRedirects = redirects;
    return this;
  }

  public HTTPRequest verbose() {
    this.verbose = true;
    return this;
  }

  public HTTPRequest addHeader(String key, String value) {
    this.headers.append(key + ": " + value + LSEP);
    return this;
  }

  public HTTPRequest addHeader(String key, Integer value) {
    this.headers.append(key + ": " + value.toString() + LSEP);
    return this;
  }

  public HTTPRequest addParameter(String key, String value) {
    try {
      // A parameter already exists, must prepend "&"
      if (this.paramString.length() > 0) {
        this.paramString.append("&" + key + "=" + URLEncoder.encode(value, "UTF-8"));
      } else {
        this.paramString.append(key + "=" + URLEncoder.encode(value, "UTF-8"));
      }
    } catch (Exception e) {
      System.out.println("Error in addParameter... " + e);
    }
    return this;
  }

  protected HTTPRequest addToBody(String content) {
    this.body.append(content);
    return this;
  }

  protected boolean send(HTTPRequest.RequestType type) {
    StringBuilder message = new StringBuilder();
    String localPath = this.path;

    // Parameters exist!
    if (this.paramString.length() > 0) { 
      localPath = localPath + "?" + this.paramString.toString(); 
    }

    if (this.body.length() > 0) {
      this.addHeader("Content-Length", this.body.length());
    }

    // Request line & host line
    message.append(String.format("%s %s %s", type.type(), this.path, HTTP_VERSION) + LSEP);
    message.append(String.format("Host: %s", this.hostName) + LSEP);
    
    // Header
    if (this.headers.length() > 0)
    {
      message.append( this.headers.toString());
    }

    // Append the final \r\n
    message.append(LSEP);

    // Body
    if (this.body.length() > 0) {
      message.append( this.body.toString());
    }

    // Send the message out
    this.outputWriter.print(message.toString());
    this.outputWriter.flush();

    if (this.verbose) {
      System.out.println("*****REQUEST*****");
      System.out.println(message.toString() + "\n");
    }

    String response = null;
    StringBuilder header = new StringBuilder();
    StringBuilder body = new StringBuilder();
    boolean bodyFound = false;
    try {
      while((response = inputReader.readLine()) != null) {
        if (!bodyFound && response.length() == 0) {
          bodyFound = true;
          continue;
        }

        if (bodyFound) {
          body.append(response + "\n");
        } else {
          header.append(response + "\n");
        }
      } 
    } catch(Exception e){
      System.out.println("Could not read input response... " + e);
    }

    this.displayResponse(header.toString(), body.toString());

    String redirectUrl = null;
    if ((redirectUrl = this.checkForRedirect(header.toString())) != null && this.allowedRedirects > 0) {
      System.out.println();
      URL url = null;
      try {
        url = new URL("http://" + redirectUrl);
        HTTPRequest redirectRequest = new HTTPRequest(url.getHost(), url.getPath() + (url.getQuery() == null ? "" : "?" + url.getQuery()), 80);
        redirectRequest.setRedirects(this.allowedRedirects - 1);
        if (this.verbose) {
          redirectRequest.verbose();
        }
        redirectRequest.send(type);
      } catch( Exception e) {
        System.out.println("Failed to create request in HTTPRequest.send(...)");
        return false;
      }
    }
    return true;
  }

  private void displayResponse(String header, String body) {
    if (header.length() == 0) {
      return;
    }
    if (this.verbose){
      System.out.println("*****RESPONSE-HEADER*****");
      System.out.println(header);
    }

    System.out.println("*****RESPONSE-BODY*****");
    System.out.println(body);
  }

  private String checkForRedirect(String header) {
    Pattern pattern = Pattern.compile(STATUS_REGEX, Pattern.CASE_INSENSITIVE);
    Pattern locationPattern = Pattern.compile(LOCATION_REGEX, Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(header);

    // Find the status code for redirection
    while(matcher.find()) {
        
      String errorCode = matcher.group(1);

      // Find the redirect location
      Matcher locationMatcher = locationPattern.matcher(header);
      if (locationMatcher.find()) {
        String newUrl = locationMatcher.group(1);
        return newUrl;
      }
    }

    return null;
  }


}