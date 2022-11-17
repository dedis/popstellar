package be.utils;

import java.io.IOException;

public interface Runnable {

  /**
   * Starts the server.
   *
   * @return true if the server has started correctly, false otherwise
   */
  boolean start() throws IOException;

  /**
   * Force server to stop by killing the process and its children if any.
   */
  void stop();

  /**
   * Checks if server is still running.
   */
  boolean isRunning();
}
