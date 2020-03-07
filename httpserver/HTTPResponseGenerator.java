import java.util.*;
import java.text.*;

public class HTTPResponseGenerator {

    private String code;
    private String reason;
    private String body;
    private HashMap<String, String> headers;

    private HTTPResponseGenerator() {
        this.code = null;
        this.reason = null;
        this.body = null;
        this.headers = new HashMap<String, String>();
        this.headers.put("Date", getDate());
        this.headers.put("Server", "COMP445FileServerV1");
    }

    public String getDate() {
        SimpleDateFormat f = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
        return f.format(new Date());
    }

    public static HTTPResponseGenerator make() {
        return new HTTPResponseGenerator();
    }

    private HTTPResponseGenerator setStatus(String code, String reason) {
        this.code = code;
        this.reason = reason;
        return this;
    }
    
    public HTTPResponseGenerator ok() {
        return this.setStatus("200", "success");
    }

    public HTTPResponseGenerator badRequest() {
        return this.setStatus("400", "Bad Request");
    }

    public HTTPResponseGenerator notFound() {
        return this.setStatus("404", "Not Found");
    }

    public HTTPResponseGenerator serverError() {
        return this.setStatus("500", "Internal Server Error");
    }

    public HTTPResponseGenerator bandwidthError() {
        return this.setStatus("509", "Bandwidth Limit Exceeded");
    }

    public HTTPResponseGenerator setBody(String body) {
        this.body = body;
        return this;
    }

    private HTTPResponseGenerator addHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public HTTPResponseGenerator asJson() {
        return this.addHeader("Content-Type", "application/json");
    }

    public HTTPResponseGenerator asXML() {
        return this.addHeader("Content-Type", "application/xml");
    }

    public HTTPResponseGenerator asHtml() {
        return this.addHeader("Content-Type", "text/html");
    }

    public HTTPResponseGenerator asText() {
        return this.addHeader("Content-Type", "text/plain");
    }

    public HTTPResponseGenerator addContentDisposition(String filename) {
        return this.addHeader("Content-Disposition", "attachment; filename=" + filename.replace("/", ""));
    }

    public String get() {
        if (!headers.containsKey("Content-Type")) {
            this.asText();
        }

        ArrayList<String> headerList = new ArrayList<String>();
        for(String k: this.headers.keySet()) {
            headerList.add(String.format("%s: %s", k, this.headers.get(k)));
        }

        return String.format(
            "HTTP/1.0 %s %s\r\n" + 
            (this.body == null ? "" : "Content-Length: " + body.length() + "\r\n") +
            (String.join("\r\n", headerList)) + (headerList.size() > 0 ? "\r\n" : "") +
            "\r\n" +
            "%s"
            , this.code, this.reason, (this.body == null ? "" : this.body));
    }

}