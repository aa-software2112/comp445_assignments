import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Hashtable;

class HTTPClient {
  private static HashMap<String, HashMap<String, String>> helpMenu;
  private static String COMPLETE_HELP = "\nhttpclient provides the ability to perform simple GET and POST requests provided specific input argument\n\n" +
    "Use Format:\n" + 
    "\thttpclient command [command-specific args]\n\n" +
    "The set of commands are:\n" +
    "\tget: Perform a GET request\n" + 
    "\tpost: Perform a POST request\n" + 
    "\thelp: Get help in the form \"httpclient help [get|post]\"";
  private static String GET_HELP = "\nTo perform a get request: httpclient get [-v] [-h key:value] url\n\n" +
    "Description: Performs a get request over a given url\n\n" +
    "Parameters:\n" +
    "\t-v: verbose output (includes full request, respose)\n" +
    "\t-h key:value: an additional header key-value pair to add - can repeat this command\n" +
    "\turl: The full URL for which the GET will be performed (i.e. http://httpbin.org/get) and can include query parameters";
  private static String POST_HELP = "\nTo perform a post request: httpclient post [-v] [-h key:value] [-d inline-data] [-f file] url\n\n" +
  "Description: Performs a post request over a given url with request body data sources from either the command line (-d) or file (-f)\n\n" +
  "Parameters:\n" +
  "\t-v: verbose output (includes full request, respose)\n" +
  "\t-h key:value: an additional header key-value pair to add - can repeat this command\n" +
  "\turl: The full URL for which the POST will be performed (i.e. http://httpbin.org/post)\n" +
  "\t-d data-string: A string of data to include in the POST body (i.e. -d \'This data goes into POST body\'\n" +
  "\t-f file-path: Filename for input including extension (i.e. input.txt)\n" +
  "\tNOTE: Only one of -d and -f can be used at once";

  private static String INVALID_INPUT = "Invalid input command, try httpclient help for more information";
  private static String COMMAND_REGEX = "(help get|help post|help|get|post)\\s*(.*)";
  private static String GET_ARGS_REGEX = "(-v|-h [^\\s]*:[^\\s]*|http[s]?://[^\\s']*|-d [^-]*|-f [^\\s]*|-o [^\\s]*)";

  private class ParsedResults {
    private boolean verbose;
    private HashMap<String, String> headers;
    private String urlString;
    private String inlineData;
    private String filePath;
    private String outputFilePath;
    private HTTPRequest.RequestType requestType;

    ParsedResults() {
      verbose = false;
      headers = new HashMap<String, String>();
      urlString = null;
      inlineData = null;
      filePath = null;
      outputFilePath = null;
      requestType = HTTPRequest.RequestType.NONE;
    }

    public boolean isVerbose() { return this.verbose; }
    public void makeVerbose() { this.verbose = true; }
    public void addHeader(String key, String value) { this.headers.put(key, value); }
    public void setUrl(String u) { this.urlString = u; }
    public void setInlineData(String d) { this.inlineData = d; }
    public void setFilePath(String f) { this.filePath = f; }
    public void setOutputFilePath(String f) { this.outputFilePath = f; }
    public void setAsGET() { this.requestType = HTTPRequest.RequestType.GET; }
    public void setAsPOST() { this.requestType = HTTPRequest.RequestType.POST; }

    public String getUrl() { return this.urlString; }
    public String getInlineData() { return this.inlineData; }
    public String getFilePath() { return this.filePath; }
    public String getOutputFilePath() { return this.outputFilePath; }
    public boolean fileOutputRequested() { return this.getOutputFilePath() != null; }
    public HashMap<String, String> getHeaders() { return this.headers; }

    public boolean isGet() { return this.requestType == HTTPRequest.RequestType.GET; }
    public boolean isPost() { return this.requestType == HTTPRequest.RequestType.POST; }
    public boolean isValidRequest() {
      if (this.requestType == HTTPRequest.RequestType.GET) {
        return this.isValidGetRequest();
      } else if (this.requestType == HTTPRequest.RequestType.POST) {
        return this.isValidPostRequest();
      } else {
        return false;
      }
    }

    private boolean isValidGetRequest() { return urlString != null && inlineData == null && filePath == null; }
    private boolean isValidPostRequest() { return urlString != null && !(inlineData != null && filePath != null); }

  }

  public static void execute(String args[]) {
    
    HTTPClient.setup();

    ParsedResults results = HTTPClient.processArgs(args);
    if (results == null) {return;}
    if (!results.isValidRequest()) { 
      System.out.println("Arguments invalid... please try \"help [get|post]\"");
      return; 
    }

    HTTPClient.carryOutRequest(results);
  }

  public static void setup() {
    helpMenu = new HashMap<String, HashMap<String, String>>();
    helpMenu.put("help", new HashMap<String, String>());
    HashMap<String, String> innerHelp = helpMenu.get("help");
    innerHelp.put("complete-help", COMPLETE_HELP);
    innerHelp.put("get", GET_HELP);
    innerHelp.put("post", POST_HELP);
  }

  public static ParsedResults processArgs(String args[]) {
    Integer nargs = args.length;
    String fullCommand = String.join(" ", args);

    Pattern pattern = Pattern.compile(COMMAND_REGEX, Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(fullCommand);

    // Looking for 2 groups
    if(nargs == 0 || (matcher.find() && matcher.groupCount() != 2)) {
      System.out.println(INVALID_INPUT);
      return null;
    }

    // Contains the "command" to execute, followed by the arguments of that command
    String command = matcher.group(1).toLowerCase();
    String commandArgs = matcher.group(2);

    // Is a help command
    if (command.contains("help")) {
      displayHelp(command);
      return null;
    }

    ParsedResults pr = HTTPClient.handleArguments(commandArgs);
    if (command.contains("get")) { pr.setAsGET();} 
    else if (command.contains("post")) { pr.setAsPOST();}
    return pr;
  }

  public static void carryOutRequest(ParsedResults results) {
    HTTPRequest request = null;

    if (results.isGet()) {
      request = GET.make(results.getUrl());
      if (request == null) {
        return;
      } 
    } else if (results.isPost()) {
      request = POST.make(results.getUrl());
      if (request == null) {
        return;
      } 
    } else {return;}

    // Add each header
    HashMap<String, String> headers = results.getHeaders();
    for(String key: headers.keySet()) {
      request.addHeader(key, headers.get(key));
    }

    // Add body - this will only execute if it is a POST as a GET will not have these fields
    String body = null;
    String path = null;
    if ((body = results.getInlineData()) != null) {
      request.addToBody(body);
    } else if ((path = results.getFilePath()) != null) {
      
      File f = new File(path);
      if (!f.isFile() || !f.exists() || !f.canRead()) {
        System.out.println(String.format("%s is NOT a file, or does not exist, or cannot be read!\n", path));
        return;
      }

      try {
        BufferedReader fileContainingBody = new BufferedReader(new FileReader(f));
        String line = null;
        StringBuilder buffer = new StringBuilder();

        while((line = fileContainingBody.readLine()) != null) {
          buffer.append(line + "\n");
        }
        body = buffer.toString();
        request.addToBody(body);
        fileContainingBody.close();
      } catch (Exception e) {
        System.out.println("Failed to read from file... " + e);
        return;
      }
    }

    HTTPResponse response = request.send();
    // The response was obtained
    if (response.validResponse()) {

      // Display it on output file
      if (results.fileOutputRequested()) {
        String outputPath = results.getOutputFilePath();
        
        try {
          PrintWriter writer = new PrintWriter (new File(outputPath));
          if (results.isVerbose()) {
            writer.println(response.getHeaders());
          }
          writer.println(response.getBody());
          writer.flush();
          writer.close();
        } catch(Exception e) {
          System.out.println(String.format("Could not open file %s for writing", outputPath));
        }

      } else { // Display it on console
        if (results.isVerbose()) {
          System.out.println(response.getHeaders());
        }
        System.out.println(response.getBody());
      }
    } else { System.out.println("No response available...");}
  }

  public static ParsedResults handleArguments(String args) {
    Pattern pattern = Pattern.compile(HTTPClient.GET_ARGS_REGEX, Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(args);
    ParsedResults parsedResults = new HTTPClient(). new ParsedResults();
    while (matcher.find()) {
      // Get the matching string from command-line arguments
      String argument = matcher.group();
      // Check which argument, process accordingly
      if (argument.contains("http")) {
        parsedResults.setUrl(argument);
      } else if (argument.contains("-v")) {
        parsedResults.makeVerbose();
      } else if (argument.contains("-h")) {
        String keyValue = argument.substring(argument.indexOf(" ") + 1);
        String keyValueArray[] = keyValue.split(":");
        parsedResults.addHeader(keyValueArray[0], keyValueArray[1]);
      } else if (argument.contains("-d")) {
        parsedResults.setInlineData(argument.substring(argument.indexOf(" ") + 1));
      } else if (argument.contains("-f")) {
        parsedResults.setFilePath(argument.substring(argument.indexOf(" ") + 1));
      } else if (argument.contains("-o")) {
        parsedResults.setOutputFilePath(argument.substring(argument.indexOf(" ") + 1));
      }
    }

    return parsedResults;
  }

  private static void displayHelp(String helpCommand) {
    if (helpCommand.equals("help get")) {
      System.out.println(GET_HELP);
    } else if (helpCommand.equals("help post")) {
      System.out.println(POST_HELP);
    } else if (helpCommand.equals("help")) {
      System.out.println(COMPLETE_HELP);
    }
  }

}