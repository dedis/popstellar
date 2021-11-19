package pop.utils;

import java.io.File;
import java.io.IOException;

public class ServerLaunch {

    /** 
     * Main server process (may have children processes)
     * For scala cmd ~> sbt ~> Server
     * For go ???
    */
    private static Process process;
    
    /**
     * Builds a server process
     * @param cmd command in format [cmd, args..]
     * @param pathWork directory to execute the command
     * @param logPath path to output log files, if null display logs in STDout & STDerr
     * @return Prcoess as Builder instance
     */
    private static ProcessBuilder serverProcess(String[] cmd, String pathWork, String logPath){
        File workDir = new File(pathWork);
        ProcessBuilder pb = new ProcessBuilder(cmd).directory(workDir);

        if(logPath == null){
            pb = pb.inheritIO();
        }else {
            File logFile = new File(logPath);
            pb = pb.redirectOutput(logFile);
            pb = pb.redirectError(logFile);
        }
        return pb;
    }

    /**
     * Runs the server command in specified directory 
     * @param cmd: command to launch
     * @param pathWork: directory to execute server
     * @return true iff the server has started correctly
     */
    public static boolean startServer(String[] cmd, String pathWork){
        return startServer(cmd, pathWork, null);
    }

    public static boolean startServer(String[] cmd, String pathWork, String logPath){
        try{
           ProcessBuilder serverProcess = serverProcess(cmd, pathWork, logPath);
           process = serverProcess.start(); 
           return true;
        }
        catch(IOException e){
            System.out.println(String.format("Could not execute %s in directory %s",cmd,pathWork));
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     *  Force server to stop by killing the process and its children if any
     */
    public static void stopServer() {
        process.children().forEach(p -> p.destroyForcibly());
        process.destroyForcibly();
    }
    
}
