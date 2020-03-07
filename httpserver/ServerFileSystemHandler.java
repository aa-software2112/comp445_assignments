import java.util.HashMap;
import java.io.File;
import java.util.ArrayList;
// https://github.com/stleary/JSON-java
import org.json.*;
// https://dom4j.github.io/
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import java.util.regex.*;
// File IO
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
// Reentrant lock
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class ServerFileSystemHandler {

    private static String searchPath = ".";
    private static String PATH_TO_FILE_REGEX = "([\\d\\w]+\\.?(\\w+))";
    public static HashMap<String, ReentrantReadWriteLock> fileLocks = new HashMap<String, ReentrantReadWriteLock>();
    private static boolean verbose = false;

    public static void setVerbose(boolean v) {
      verbose = v;
    }

    public static void setPath(String p) {
      searchPath = p;
    }
    
    private static boolean pathAllowed(String path) {
        int backslashCount = 0;
        int forwardSlashCount = 0;

        // Count the number of forward or backslashes
        for(int i = 0; i<path.length(); i++) {
            char c = path.charAt(i);
            if (c == '/') {
                forwardSlashCount++;
            } else if (c == '\\') {
                backslashCount++;
            }
        }
        return !(backslashCount > 0 || forwardSlashCount > 1);
    }

    private static void displayFullMessage(String path,
    String method, 
    HashMap<String, String> params, 
    HashMap<String, String> headers,
    String body,
    String response) {
      if (!verbose) return;
      System.out.println(String.format("\n|***************|" +
      "\n" +
      "{%s %s %s %s %s}" +
      "\n\n" +
      "%s" +
      "\n" +
      "|**************|", path, method, params, headers, body, response));

    }

    private static void display(String msg) {
      if (!verbose) return;
      System.out.println(msg);
    }

    public static String clientHandler(String path,
        String method, 
        HashMap<String, String> params, 
        HashMap<String, String> headers,
        String body) 
        {
            String response = null;
            if (method.equals("GET")) {
                if (path.equals("/")) {
                    response = ServerFileSystemHandler.listDirectory(path, method, params, headers, body);
                } else {
                    response = ServerFileSystemHandler.getFile(path, method, params, headers, body);
                }
            } else if (method.equals("POST")) {
                // Path will be passed directly to function, it will deal with invalid formats
                response = ServerFileSystemHandler.createFile(path, method, params, headers, body);
            } else {
                response = HTTPResponseGenerator.make().badRequest().setBody("Server only supports GET or POST").get();
            }
            displayFullMessage(path, method, params, headers, body, response);
            return response;
        }

    public static String listDirectory(String path,
        String method, 
        HashMap<String, String> params, 
        HashMap<String, String> headers,
        String body) 
        {
            HTTPResponseGenerator response = HTTPResponseGenerator.make();

            File curDir = new File(ServerFileSystemHandler.searchPath);
            File[] dirFiles = curDir.listFiles();
            ArrayList<String> fileNames = new ArrayList<String>(dirFiles.length);
            for(File nextfile: dirFiles) {
                fileNames.add(nextfile.getName());
                display(nextfile.getName());
            }
            
            String responseBody = null;
            // Check requested format
            if (headers.containsKey("accept")) {
                String acceptType = headers.get("accept");

                if (acceptType.equals("application/json")) {
                    JSONObject jsonFileList = new JSONObject();
                    jsonFileList.put("file_list", new JSONArray(fileNames));
                    response.setBody(jsonFileList.toString())
                        .asJson()
                        .ok();
                } else if (acceptType.equals("application/xml")) {
                    Document xmlDoc = DocumentHelper.createDocument();
                    Element fileRoot = xmlDoc.addElement("file_list");
                    for(String filename:fileNames) {
                        fileRoot.addElement("file")
                        .addText(filename);
                    }
                    response.setBody(xmlDoc.asXML())
                        .asXML()
                        .ok();
                } else if (acceptType.equals("text/html")) {
                    StringBuilder html = new StringBuilder();
                    html.append("<html>\n");
                    html.append("<body>\n");
                    for(String filename:fileNames) {
                        html.append(String.format("<h3>%s</h3>\n", filename));
                    }
                    html.append("</body>\n");
                    html.append("</html>");
                    response.setBody(html.toString())
                        .asHtml()
                        .ok();
                } else {
                    response.badRequest().setBody("Failed to include proper Accept type. Types supported: application/json, application/xml, text/html... You sent " + acceptType);
                }
            } else {
                response.setBody(String.join("\n", fileNames))
                    .ok();
            }

            return response.get();
        }

    public static String createFile(String path,
        String method, 
        HashMap<String, String> params, 
        HashMap<String, String> headers,
        String body) {
            
            HTTPResponseGenerator response = HTTPResponseGenerator.make();
            if (!pathAllowed(path)) {
                return response.badRequest().setBody(String.format("The path specified by %s is invalid\nPlease keep paths within current directory", path)).get();
            }

            WriteLock wl = null;
            synchronized(fileLocks) {
                if (!fileLocks.containsKey(path)) {
                    fileLocks.put(path, new ReentrantReadWriteLock());
                }
                wl = fileLocks.get(path).writeLock();
            }

            File requestFile = new File(searchPath + path);
            wl.lock();
            display(String.format("Writing %s...", path));
            try {
                PrintWriter w = new PrintWriter(requestFile);
                w.write(body);
                w.flush();
                w.close();
                response.ok().setBody(String.format("Wrote %d bytes to file %s", body.length(), requestFile.getName()));
            } catch(FileNotFoundException f) {
                response.notFound().setBody(String.format("Could not find file %s\nError = %s", requestFile.getName(), f));
            } 
            display(String.format("Exiting Write Block %s...", path));
            wl.unlock();
            return response.get();
        }
    
    public static String getFile(String path,
        String method, 
        HashMap<String, String> params, 
        HashMap<String, String> headers,
        String body) 
        {
            
            HTTPResponseGenerator response = HTTPResponseGenerator.make();
            if (!pathAllowed(path)) {
                return response.badRequest().setBody(String.format("The path specified by %s is invalid\nPlease keep paths within current directory", path)).get();
            }

            ReadLock rl = null;
            synchronized(fileLocks) {
                if (!fileLocks.containsKey(path)) {
                    fileLocks.put(path, new ReentrantReadWriteLock());
                }
                rl = fileLocks.get(path).readLock();
            }

            File requestFile = new File(searchPath + path);
            BufferedReader w = null;
            rl.lock();
            display(String.format("Reading %s...", path));
            display("\t# of readers = " + fileLocks.get(path).getReadLockCount());
            try {
                w = new BufferedReader(new FileReader(requestFile));
                StringBuilder b = new StringBuilder();
                String line = null;
                while( (line = w.readLine()) != null ) {
                    b.append(line);
                }
                response.setBody(b.toString()).addContentDisposition(path).ok();
            } catch(FileNotFoundException e) {
                response.notFound().setBody(String.format("File %s could not be found\nError = %s", path, e));
            } catch(IOException io) {
                response.serverError().setBody(String.format("Failed during reading of file stream %s\nError = %s", path, io));
            } finally {
                try {
                    if (w != null)
                        w.close();
                } catch(IOException ioclose) {
                    // This is OK
                    System.out.println("Failed to close file");
                }
            }
            display(String.format("\tFinished Reading %s...", path));
            rl.unlock();
            display(String.format("\t# remaining readers = %d", fileLocks.get(path).getReadLockCount()));

            return response.get();
        }
}