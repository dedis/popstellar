package be.utils;

public interface Configurable {

  /**
   * Returns the command to start the server.
   */
  String[] getCmd();

  /**
   * Returns the working directory.
   */
  String getDir();

  /**
   * Returns the path to output log files.
   */
  String getLogPath();

  /**
   * Deletes database of the server
   */
  void deleteDatabaseDir();
}
