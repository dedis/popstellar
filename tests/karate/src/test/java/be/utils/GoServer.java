package be.utils;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;


public class GoServer extends Server implements Configurable {
  private String dbPath;

  public GoServer(String host, int clientPort, int serverPort, int authPort, String dbPath, String logPath) {
    super(host, clientPort, serverPort, authPort, logPath);
    this.dbPath = dbPath;
  }

  public GoServer() {
    this("localhost", 9000, 9001, 9100, Paths.get("..", "..", "be1-go", "database-a").toString(), null);
  }

  @Override
  public String[] getCmd() {
    Map<String, String> args = new HashMap<>();
    args.put("client-port", String.valueOf(clientPort));
    args.put("server-port", String.valueOf(serverPort));
    args.put("auth-port", String.valueOf(authPort));
    args.put("server-public-address", host);
    args.put("server-listen-address", host);

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
    System.out.println("No database to delete");
  }
}
