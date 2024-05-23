package be.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.Comparator;

public abstract class Server implements Runnable {
  // Main server process (may have children processes)
  private Process process;
  private String logPath;
  protected String host;
  protected int clientPort;
  protected int serverPort;
  protected int authPort;
  protected List<String> peers  = new ArrayList<>();

  public Server(String host, int clientPort, int serverPort, int authPort) {
    this(host, clientPort, serverPort, authPort, null);
  }

  public Server(String host, int clientPort, int serverPort, int authPort, String logPath) {
    super();
    this.host = host;
    this.clientPort = clientPort;
    this.serverPort = serverPort;
    this.authPort = authPort;
    this.logPath = logPath;
  }

  public void addPeer(Server peer) {
    peers.add(peer.host + ":" + peer.serverPort);
  }

  public int getServerPort() {
    return serverPort;
  }

  public String getHost() {
    return host;
  }

  /**
   * Builds a server process
   *
   * @param cmd     command in format [cmd, args..]
   * @param dir     directory to execute the command
   * @param logPath path to output log files, if null display logs in STDout & STDerr
   * @return Process as Builder instance
   */
  private static ProcessBuilder build(String[] cmd, String dir, String logPath) {
    File workingDirectory = new File(dir);
    ProcessBuilder processBuilder = new ProcessBuilder(cmd).directory(workingDirectory);

    if (logPath == null) {
      processBuilder.inheritIO();
    } else {
      File logFile = new File(logPath);
      ProcessBuilder.Redirect redirect = ProcessBuilder.Redirect.appendTo(logFile);
      processBuilder
        .redirectOutput(redirect)
        .redirectError(redirect);
    }
    return processBuilder;
  }

  @Override
  public void stop() throws RuntimeException {
    System.out.println("Stopping server...");
    if (process == null) {
      System.out.println("Server already stopped");
      return;
    }

    process.descendants().forEach(ProcessHandle::destroy);
    process.destroy();
    List<CompletableFuture<?>> allProcesses = process
      .descendants()
      .map(ProcessHandle::onExit)
      .collect(Collectors.toList());

    allProcesses.add(process.onExit());

    CompletableFuture<Void> onExit = CompletableFuture
      .allOf(allProcesses.toArray(new CompletableFuture[]{}));

    try {
      // Wait for the process to exit
      onExit.get(1, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    } catch (TimeoutException e) {
      System.out.println("Could not stop, trying harder");
      forceStop(onExit);
    }

    process = null;
    System.out.println("Server stopped");
  }

  private void forceStop(CompletableFuture<Void> onExit) {
    // The process did not exit, trying the force
    process.descendants().forEach(ProcessHandle::destroyForcibly);
    process.destroyForcibly();
    try {
      onExit.get(1, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException ex) {
      throw new RuntimeException("Could not exit the server correctly", ex);
    }
  }

  @Override
  public boolean isRunning() {
    return process.isAlive();
  }

  @Override
  public boolean start() throws IOException {
    return start(getCmd(), getDir(), getLogPath());
  }

  public abstract String[] getCmd() throws IOException;

  public abstract String getDir();

  /**
   * Runs the server command in specified directory
   *
   * @param cmd command to launch
   * @param dir directory to execute server
   * @return true if the server has started correctly, false otherwise
   */
  public boolean start(String[] cmd, String dir) {
    return start(cmd, dir, null);
  }

  /**
   * Runs the server command in specified directory
   *
   * @param cmd     command to launch
   * @param dir     directory to execute server
   * @param logPath path to output log files
   * @return true if the server has started correctly, false otherwise
   */
  public boolean start(String[] cmd, String dir, String logPath) {
    if (cmd.length == 0) {
      System.out.println("Need a command to start the server");
      return false;
    }

    System.out.printf("Executing %s in directory %s%n",
      Stream.of(cmd).reduce((s, acc) -> s + " " + acc).get(), dir);

    try {
      ProcessBuilder serverProcess = build(cmd, dir, logPath);
      process = serverProcess.start();
      System.out.println("Process PID = " + process.pid());
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }
   /**
   * Deletes database of the server
   */
  public abstract void deleteDatabaseDir();

  public static boolean isWindowsOS() {
    return System.getProperty("os.name").toLowerCase().contains("windows");
  }

  public String getLogPath() {
    return logPath;
  }
}
