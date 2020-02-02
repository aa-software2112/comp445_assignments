
class HTTPResponse {
  private String responseHeaders;
  private String responseBody;

  HTTPResponse() {
    this.responseHeaders = null;
    this.responseBody = null;
  }

  public HTTPResponse setHeaders(String h) { 
    this.responseHeaders = h;
    return this;
  }
  public HTTPResponse setBody(String b) { 
    this.responseBody = b;
    return this;
  }
  public boolean validResponse() {
    return this.responseBody != null && this.responseHeaders != null;
  }
  public String getHeaders() { return this.responseHeaders; }
  public String getBody() { return this.responseBody; }

}