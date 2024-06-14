package be.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

public class ScalaServer extends Server implements Configurable {
  private String dbPath;

  public ScalaServer(String host, int port, String dbPath, String logPath) {
    super(host, port, port, port, logPath);
    this.dbPath = dbPath;
  }

  public ScalaServer() {
    this("127.0.0.1", 8000, null, null);
  }

  @Override
  public String[] getCmd() throws IOException {
    Path workingDirectory = Paths.get("").toAbsolutePath();
    Path tempDir = Files.createTempDirectory("scala-server");

    // Create a temporary config file
    Path configTemplate = workingDirectory.resolve("src/test/java/data/scala.template.conf");
    String config = new String(Files.readAllBytes(configTemplate));
    config = config.replace("{{host}}", host);
    config = config.replace("{{port}}", String.valueOf(serverPort));
    File configFile = new File(tempDir.toFile(), "application.conf");
    Files.write(configFile.toPath(), config.getBytes());

    // Create a temporary peer file
    File peerFile = File.createTempFile("scala-peers", ".conf");
    for (String peer : peers) {
      Files.write(peerFile.toPath(), ("ws://" + peer + "/server\n").getBytes());
    }

    if (this.dbPath == null) {
      dbPath = Files.createTempDirectory("scala-db").toString();
    }

    String securityDirPath = Paths.get("src", "security").toString();
    File targetJar = getTargetJar();

    return new String[] {
      "java",
      "-Dscala.config=" + tempDir.toString(),
      "-Dscala.peerlist=" + peerFile.getCanonicalPath(),
      "-Dscala.security=" + securityDirPath,
      "-Dscala.db=" + dbPath,
      "-jar", targetJar.getCanonicalPath()
    };
  }

  private File getTargetJar() throws IOException {
    File targetPath = new File(getDir(), "target");
    // Find the folder in target containing the built jar. It should resemble scala-x.xx
    File[] scalaFolder = targetPath.listFiles((dir, name) -> name.startsWith("scala-"));
    if (scalaFolder == null || scalaFolder.length == 0) {
      throw new FileNotFoundException("Could not find scala folder in the target folder. Did you assemble the server jar ?");
    }

    if (scalaFolder.length != 1) {
      throw new IOException("There are multiple folder matching scala-xxx, please remove the old ones.");
    }

    // Find the correct jar file in the folder
    File[] jars = scalaFolder[0].listFiles((dir, name) -> name.startsWith("pop-assembly-"));
    if (jars == null || jars.length == 0) {
      throw new FileNotFoundException("Could not find jar file in the target folder. Did you assemble it ?");
    }

    if (jars.length != 1) {
      throw new IOException("There are multiple jar file matching pop-assembly-**, please remove the old ones");
    }

    return jars[0];
  }

  @Override
  public String getDir() {
    return Paths.get("..", "..", "be2-scala").toString();
  }

  @Override
  public void deleteDatabaseDir() {
    if (dbPath == null) {
      System.out.println("No database to delete");
      return;
    }

    Path path = Paths.get(dbPath);
    try (Stream<Path> walk = Files.walk(path)) {
      walk.sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(ScalaServer::deleteHandled);
    } catch (IOException e) {
      System.err.println("Could not delete database located at " + path);
      e.printStackTrace(System.err);
    }

    assertFalse(Files.exists(path));
  }

  private static void deleteHandled(File file) {
    if (!file.delete()) {
      System.err.println("Could not delete database folder" + file.toPath());
    }
  }
}
