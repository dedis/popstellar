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

  @Override
  public boolean start() throws IOException {
    return super.start(getCmd(), getDir(), getLogPath());
  }

  @Override
  public void stop() {
    super.stop();
  }

  @Override
  public String[] getCmd() throws IOException {
    String configPath = Paths.get("src", "main", "scala", "ch", "epfl", "pop", "config").toString();
    File targetJar = getTargetJar();

    return new String[] {
      "java",
      "-Dscala.config=" + configPath,
      "-jar", targetJar.getCanonicalPath()
    };
  }

  private File getTargetJar() throws IOException {
    File targetPath = new File(getDir(), "target");
    // Find the folder in target containing the built jar. It should resemble scala-x.xx
    File[] scalaFolder = targetPath.listFiles((dir, name) -> name.startsWith("scala-"));
    if (scalaFolder == null || scalaFolder.length == 0) throw new FileNotFoundException("Could not find scala folder in the target folder. Did you assemble the server jar ?");
    if (scalaFolder.length != 1) throw new IOException("There are multiple folder matching scala-xxx, please remove the old ones.");

    File[] jars = scalaFolder[0].listFiles((dir, name) -> name.startsWith("pop-assembly-"));
    if (jars == null || jars.length == 0) throw new FileNotFoundException("Could not find jar file in the target folder. Did you assemble it ?");
    if (jars.length != 1) throw new IOException("There are multiple jar file matching pop-assembly-**, please remove the old ones");

    return jars[0];
  }

  @Override
  public String getDir() {
    return Paths.get("..", "..", "be2-scala").toString();
  }

  @Override
  public String getLogPath() {
    return Paths.get("scala.log").toString();
  }

  @Override
  public void deleteDatabaseDir() {
    System.out.println("Deleting database...");
    Path path = Paths.get("..", "..", "be2-scala", "database");
    try (Stream<Path> walk = Files.walk(path)) {
      walk.sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(ScalaServer::deleteHandled);
    } catch (IOException e) {
      System.err.println("Could not delete database located at " + path);
    }

    assertFalse(Files.exists(path));
  }

  private static void deleteHandled(File file) {
    if (!file.delete()) {
      System.err.println("Could not delete database folder" + file.toPath());
    }
  }
}
