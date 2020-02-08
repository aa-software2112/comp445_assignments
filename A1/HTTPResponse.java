
class HTTPResponse {
  private String responseHeaders;
  private String responseBody;
  private String associatedRequest;

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
  public HTTPResponse setAssociatedRequest(String reqMsg) {
    this.associatedRequest = reqMsg;
    return this;
  }
  public boolean validResponse() {
    return this.responseBody != null && this.responseHeaders != null;
  }
  public String getHeaders() { return this.responseHeaders; }
  public String getBody() { return this.responseBody; }
  public String getAssociatedRequest() { return this.associatedRequest; }

}