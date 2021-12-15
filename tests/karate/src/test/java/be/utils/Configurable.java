package be.utils;

public interface Configurable {
  /**
   * Maximum attempts to delete/reset database before failing
   */
  final int MAX_DB_DELETE_ATTEMPTS = 3;
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
