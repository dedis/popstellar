package be.utils;

import java.nio.file.Paths;

public class GoServer extends Server implements Configurable {

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
    return new String[]{"bash", "-c", "./pop organizer --pk J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM= serve"};
  }

  @Override
  public String getDir() {
    return Paths.get("..", "..", "be1-go").toString();
  }

  @Override
  public String getLogPath() {
    return Paths.get("go.log").toString();
  }

  @Override
  public void deleteDatabaseDir() {
    // TODO Auto-generated method stub

  }

}
