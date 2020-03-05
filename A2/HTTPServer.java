import java.net.*;
import java.io.*;
import java.lang.StringBuilder;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.Method;
import java.lang.reflect.*;

public class HTTPServer extends Thread {
    private ServerSocket httpServer;
    private Integer listeningPort;
    private Method handler;
    public static Class[] HandlerInterface = 
    {
        String.class,
        String.class,
        HashMap.class,
        HashMap.class,
        String.class
    };

    public HTTPServer(Integer port) {
        this.listeningPort = port;
        this.handler = null;
    }

    public void registerHandler(Method toExecute) {
        this.handler = toExecute;
        System.out.println(this.handler.getParameterCount());
    }

    public void run() {

        try{
            // Wait for a client request, assign it to another port
            httpServer = new ServerSocket(this.listeningPort);
        } catch (IOException e) {
                System.out.println("Could not open server socket over port " + this.listeningPort);
                System.out.println(e);
                System.exit(0);
        }
                
        // Keep accepting client requests
        while(true) {
            try{
                System.out.println("Awaiting connections over port " + this.listeningPort);
                Socket clientSocket = httpServer.accept(); // Blocking call
                PrintWriter clientInput = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader clientOutput = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                System.out.println("Received connection from " + clientSocket);
                HandleClientRequests newRequest = new HandleClientRequests(clientInput, clientOutput, clientSocket);
                newRequest.start();
            } catch (IOException e) {
                System.out.println(e);
                System.exit(0);
            }
        }
    }

    class HandleClientRequests extends Thread {
        private PrintWriter clientInput;
        private BufferedReader clientOutput;
        private Socket clientSocket;
        private static final String STATUS_LINE_REGEX = "(GET|POST) (\\/\\S*) (HTTP\\/\\d\\.\\d)";
        private static final String HEADER_REGEX = "(\\S+): (\\S+)";
        private static final String QUERY_PARAMS_REGEX = "([^\\s&\\?]+)?=([^\\s&]+)";
        
        private String method;
        private String path;
        private String version;
        private HashMap<String, String> params;
        private HashMap<String, String> headers;
        private String body;

        HandleClientRequests(PrintWriter ci, BufferedReader co, Socket cs) {
            this.clientInput = ci;
            this.clientOutput = co;
            this.clientSocket = cs;
            this.headers = new HashMap<String, String>();
            this.params = new HashMap<String, String>();
            this.method = new String();
            this.path = new String();
            this.version = new String();
            this.body = new String();
        }

        public void run() {
            StringBuilder requestString = new StringBuilder(1024);

            // Read line by line, parsing as we go
            try {
                // First line should contain GET/POST status
                String line = this.clientOutput.readLine();
                if (!parseRequestLine(line)) {
                    System.out.println("Status line failed");
                    return; // jumps to finally
                }

                // Continuously parse headers until blank \r\n line
                boolean headerFailure = false;
                boolean headersComplete = false;
                while(!headerFailure && !headersComplete) { 

                    line = this.clientOutput.readLine();
                    if (line.length() > 0 ) { // is a header line
                        if (!parseHeader(line)) {
                            System.out.println("Failed to parse header...");
                            headerFailure = true;
                        }
                    } else { // Is the \r\n splitting headers and entity-body
                        headersComplete = true;
                    }
                }

                // Failed to read what should have been a header
                if (headerFailure) {
                    return;
                }

                // As a GET, we execute the command in "path", as a POST we read the entity body 
                // if the content-length exists then execute the command in "path"
                if (this.method.equals("POST")) {
                   if(!readBody()) {
                       System.out.println("Failed to read body...");
                       return;
                   }
                }

                // Handle the request
                try {
                    String response = (String)HTTPServer.this.handler.invoke(null, this.path, this.method, this.params, this.headers, this.body);
                    this.clientInput.write(response);
                    this.clientInput.flush();
                }catch(InvocationTargetException e) {
                    System.out.println("Could not invoke method " + HTTPServer.this.handler.getName());
                    System.out.println(e.getTargetException());
                    return;
                } catch(IllegalAccessException a) {
                    System.out.println(a);
                    return;
                }

            } catch(IOException e) {
                System.out.println("Falied to read client socket ... " + e);
            } finally {
                try {
                    this.clientSocket.close();
                } catch(IOException q) {
                    System.out.println(q);
                }
            }
        }

        private boolean parseRequestLine(String line) {
            Pattern pattern = Pattern.compile(STATUS_LINE_REGEX, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(line);

            // Failed to find a match
            if (!matcher.lookingAt()) {
                return false;
            }

            this.method = matcher.group(1);
            String originalPath = matcher.group(2);
            if (originalPath.indexOf("?") != -1) { // Remove the query string from path if exists
                this.path = originalPath.split("\\?")[0];
            } else {
                this.path = originalPath;
            }
            this.version = matcher.group(3);

            // Attempt query string parsing
            pattern = Pattern.compile(QUERY_PARAMS_REGEX, Pattern.CASE_INSENSITIVE);
            matcher = pattern.matcher(originalPath);
            while(matcher.find()) {
                this.params.put(matcher.group(1), matcher.group(2));
            }

            return true;
        }

        private boolean parseHeader(String line) {
            Pattern pattern = Pattern.compile(HEADER_REGEX, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(line);

            if(!matcher.lookingAt()) {
                return false;
            }

            String headerKey = matcher.group(1).toLowerCase();
            String headerValue = matcher.group(2).toLowerCase();
            this.headers.put(headerKey, headerValue);
            return true;
        }

        private boolean readBody() {
             if (this.headers.containsKey("content-length")) {
                int lengthToRead = -1;
                int bytesRead = -1;

                // Fetch the content-length
                try {
                    lengthToRead = Integer.parseInt(this.headers.get("content-length"));
                } catch(NumberFormatException invalidIntegerString) { // Content-length is invalid!
                    System.out.println(invalidIntegerString);
                    return false;
                }

                // Read the body
                char[] buff = new char[lengthToRead];
                try {
                    bytesRead = this.clientOutput.read(buff, 0, lengthToRead);
                } catch(IOException e) {
                    System.out.println("Failed to read body... " + e);
                    return false;
                }

                // Failed to read the full body
                System.out.println(bytesRead);
                System.out.println(lengthToRead);
                if (!(bytesRead == lengthToRead)) {
                    System.out.println(String.format("Bytes read %d doesn't match content-length %d", bytesRead, lengthToRead));
                    return false;
                }

                this.body = new String(buff);
                return true;
             } else { // If there is no content length, may be a POST without any data...
                return true;
             }
        }
    }


}