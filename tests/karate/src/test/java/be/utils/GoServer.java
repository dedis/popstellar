package be.utils;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Files;
import java.io.IOException;

public class GoServer extends Server implements Configurable {
  private String dbPath;

  public GoServer(String host, int clientPort, int serverPort, int authPort, String dbPath, String logPath) {
    super(host, clientPort, serverPort, authPort, logPath);
    this.dbPath = dbPath;
  }

  public GoServer() {
    this("localhost", 9000, 9001, 9100, null, null);
  }

  @Override
  public String[] getCmd() throws IOException {
    Map<String, String> args = new HashMap<>();
    args.put("client-port", String.valueOf(clientPort));
    args.put("server-port", String.valueOf(serverPort));
    args.put("auth-port", String.valueOf(authPort));
    args.put("server-public-address", host);
    args.put("server-listen-address", host);

    if (dbPath == null) {
      dbPath = Files.createTempFile("go_database", ".sqlite").toString();
    }
    args.put("database-path", dbPath);

    if (peers.size() > 0) {
      args.put("other-servers", String.join(",", peers));
    }

    String cmd = "server serve";
    for (Map.Entry<String, String> entry : args.entrySet()) {
      cmd += " --" + entry.getKey() + " " + entry.getValue();
    }

    if (isWindowsOS()) {
      return new String[]{
        "cmd",
        "/c",
        "\"pop.exe " + cmd + "\""
      };
    } else {
      return new String[]{
        "bash",
        "-c",
        "./pop " + cmd
      };
    }
  }

  @Override
  public String getDir() {
    return Paths.get("..", "..", "be1-go").toString();
  }

  @Override
  public void deleteDatabaseDir() {
    if (dbPath != null) {
      try {
        Files.deleteIfExists(Paths.get(dbPath));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
