import java.io.File;
import java.util.HashMap;

public class HTTPFS {
    
    private static String COMPLETE_HELP = "\nhttpfs - basic file server\n" +
    "Use Format:\n" + 
    "\thttpfs [-v] [-p PORT] [-d PATH-TO-DIR]\n\n" +
    "\n" +
    String.format("%-10s Verbose output (debugging)\n", "\t-v") + 
    String.format("%-10s Port over which server listens for requests\n", "\t-p") + 
    String.format("%-10s Deployment file-directory. All requests target this directory.", "\t-d");
    
    private static Integer port = 8080;
    private static boolean verbose = false;
    private static String searchPath = ".";

    private static boolean isHelp(String[] args){
        for(String s:args){
            if (s.toLowerCase().equals("help")) {
                return true;
            }
        }
        return false;
    }

    private static boolean setVerbose(String input) {
        verbose = true;
        return true;
    }

    private static boolean processPort(String input) {
        try {
            HTTPFS.port = Integer.parseInt(input);
        } catch(NumberFormatException e) {
            System.out.println(e);
            return false;
        }
        return true;
    }

    private static boolean processPath(String input) {
        String fin = input.endsWith("/") ? input.substring(0, input.length()-1) : input + "/";
        fin = fin.charAt(0) == '/' ? "." + fin : fin;
        fin = (fin.charAt(0) != '.' && fin.charAt(1) != '/') ? "./" + fin : fin;
        File directory = new File(fin);
        if (directory.isDirectory())
        {
            HTTPFS.searchPath = fin;
            return true;
        } else {
            return false;
        }
    }
    class CommandMethod {
        public boolean execute(String arg) {
            return false;
        }
    }

    class PathCmd extends HTTPFS.CommandMethod {
        public boolean execute(String arg) {
            return HTTPFS.processPath(arg);
        }
    }

    class PortCmd extends HTTPFS.CommandMethod {
        public boolean execute(String arg) {
            return HTTPFS.processPort(arg);
        }
    }

    class VerboseCmd extends HTTPFS.CommandMethod {
        public boolean execute(String arg) {
            return HTTPFS.setVerbose(arg);
        }
    }

    public static void parseArgs(String[] args){
        if (isHelp(args)) {
            System.out.println(COMPLETE_HELP);
            System.exit(1);
        }
        int argIdx = 0;
        HashMap<String, HTTPFS.CommandMethod> commands = new HashMap<String, HTTPFS.CommandMethod>();
        HTTPFS h = new HTTPFS();
        commands.put("-v", h.new VerboseCmd());
        commands.put("-p", h.new PortCmd());
        commands.put("-d", h.new PathCmd());
        try {        
            while(argIdx < args.length) {
                String arg = args[argIdx];
                if (commands.containsKey(arg)) {
                    if (arg.equals("-v")) {
                        commands.get(arg).execute("");
                    } else {
                        commands.get(arg).execute(args[++argIdx]);
                    }
                } else {
                    System.out.println("Invalid argument = " + arg);
                    System.exit(1);
                }
                argIdx++;
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            System.out.println("Incorrect argument format... " + e);
            System.exit(1);
        }
        
        System.out.printf("Starting server:\n\tVerbose: %b \n\tPort: %d \n\tPath: %s\n", verbose, port, searchPath);
        return;
    }

    public static void main(String[] args) {
        parseArgs(args);
        ServerFileSystemHandler.setVerbose(verbose);
        ServerFileSystemHandler.setPath(searchPath);
        HTTPServer s = new HTTPServer(port);
        try {
            s.registerHandler(ServerFileSystemHandler.class.getMethod("clientHandler", HTTPServer.HandlerInterface));
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