import java.util.HashMap;

public class driver {

    /**
     returns a list of the current files in the data directory. You can return different
    type format such as JSON, XML, plain text, HTML according to the Accept key of the
    Comp445 – Lab Assignment # 2 Page 3
    header of the request. However, this is not mandatory; you can simply ignore the
    header value and make your server always returns the same output.
    */
    public static String clientHandler(String path,
        String method, 
        HashMap<String, String> params, 
        HashMap<String, String> headers,
        String body) 
        {
            System.out.println(String.format("%s %s %s %s %s", path, method, params, headers, body));
            return "";
        }

    public static void main(String[] args) {
        HTTPServer s = new HTTPServer(8080);
        try {
            s.registerHandler(driver.class.getMethod("clientHandler", HTTPServer.HandlerInterface));
        } catch(Exception r) {
            System.out.println(r);
            System.exit(0);
        }
        s.start();
        try {
            s.join();
        } catch(Exception e) {
            System.out.println(e);
        }
    }
}