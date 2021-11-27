package be.utils;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

public abstract class Server implements Runnable {

    // Main server process (may have children processes)
    private Process process;

    @Override
    public void stop() {
        process.children().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    @Override
    public boolean isRunning() {
        return process.isAlive();
    }

    /**
     * Runs the server command in specified directory 
     * @param cmd command to launch
     * @param dir directory to execute server
     * @return true if the server has started correctly, false otherwise
     */
    public boolean start(String[] cmd, String dir) {
        return start(cmd, dir, null);
    }

    /**
     * Runs the server command in specified directory
     * @param cmd command to launch
     * @param dir directory to execute server
     * @param logPath path to output log files
     * @return true if the server has started correctly, false otherwise
     */
    public boolean start(String[] cmd, String dir, String logPath) {
        try{
           ProcessBuilder serverProcess = build(cmd, dir, logPath);
           process = serverProcess.start(); 
           System.out.println("Process PID = " + process.pid());
           return true;
        }
        catch(IOException e) {
            System.out.printf("Could not execute %s in directory %s%n",
                    Stream.of(cmd).reduce((s,acc)-> s + " " + acc).orElse("No command found!"),
                    dir);
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     * Builds a server process
     * @param cmd command in format [cmd, args..]
     * @param dir directory to execute the command
     * @param logPath path to output log files, if null display logs in STDout & STDerr
     * @return Process as Builder instance
     */
    private ProcessBuilder build(String[] cmd, String dir, String logPath) {
        File workingDirectory = new File(dir);
        ProcessBuilder processBuilder = new ProcessBuilder(cmd).directory(workingDirectory);

        if(logPath == null) {
            processBuilder = processBuilder.inheritIO();
        } else {
            File logFile = new File(logPath);
            processBuilder = processBuilder.redirectOutput(logFile);
            processBuilder = processBuilder.redirectError(logFile);
        }
        return processBuilder;
    }
}
