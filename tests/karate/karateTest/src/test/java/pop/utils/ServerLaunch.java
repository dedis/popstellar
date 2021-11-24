package pop.utils;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

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
     * @param workDirPath directory to execute the command
     * @param logPath path to output log files, if null display logs in STDout & STDerr
     * @return Prcoess as Builder instance
     */
    private static ProcessBuilder serverProcess(String[] cmd, String workDirPath, String logPath){
        File workDir = new File(workDirPath);
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
    public static boolean startServer(String[] cmd, String workDirPath){
        return startServer(cmd, workDirPath, null);
    }

    //TODO: Check input cmd not empty/null and non null empty path
    public static boolean startServer(String[] cmd, String workDirPath, String logPath){
        try{
           ProcessBuilder serverProcess = serverProcess(cmd, workDirPath, logPath);
           process = serverProcess.start(); 
           System.out.println("Process PID = "+process.pid());
           return true;
        }
        catch(IOException e){
            System.out.println(
                String.format("Could not execute %s in directory %s",
                Stream.of(cmd).reduce((s,acc)-> s + " " + acc).orElseGet(() ->"No command found !"),
                workDirPath)
                );
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

    /**
     *  Checks if server is still running
     */
    public static boolean isRunning(){
        return process.isAlive();
    }
    
}
