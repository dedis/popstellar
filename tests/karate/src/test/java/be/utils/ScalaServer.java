package be.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

public class ScalaServer extends Server implements Configurable {

  @Override
  public boolean start() {
    return super.start(getCmd(), getDir(), getLogPath());
  }

  @Override
  public void stop() {
    super.stop();
  }

  @Override
  public String[] getCmd() {
    String configPath = Paths.get("src", "main", "scala", "ch", "epfl", "pop", "config").toString();
    return new String[] { "sbt.bat", "-Dscala.config=" + configPath, "run" };
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
    System.out.println("Deleting...");
    Path path = Paths.get("..", "..", "be2-scala", "database");
    try (Stream<Path> walk = Files.walk(path)) {
      walk.sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(ScalaServer::deleteHandled);
    } catch (IOException e) {
      System.err.println("Could not delete database located at " + path.toString());
    }
    assertFalse(Files.exists(path));
  }

  private static void deleteHandled(File file){
    int i = 0;
    while(i < Configurable.MAX_DB_DELETE_ATTEMPTS && !file.delete()){
      try{
        //Wait for the server to realease the resource(database)
        //and try again
        Thread.sleep(1500);
        i++;
      }
      catch(InterruptedException e){
        System.err.println("Delete thread of file " + file.toPath() + " was interrupted");
      }
    }
    if(i >= Configurable.MAX_DB_DELETE_ATTEMPTS){
      System.err.println("Could not delete database folder" + file.toPath());
    }
  }
}
