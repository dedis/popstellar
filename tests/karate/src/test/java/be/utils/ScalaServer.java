package be.utils;

import java.nio.file.Paths;

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
    return new String[]{"sbt", "-Dscala.config=" + configPath, "run"};
  }

  @Override
  public String getDir() {
    return Paths.get("..", "..", "be2-scala").toString();
  }

  @Override
  public String getLogPath() {
    return Paths.get("scala.log").toString();
  }
}
