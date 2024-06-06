package be.utils;

import common.utils.MockClient;

import java.util.List;
import java.io.IOException;

public abstract class ServerNetwork {
  public abstract List<Server> getServers();
  public abstract MockClient createInputNodeClient();
  public abstract MockClient createOutputNodeClient();
  public abstract Server getInputNode();
  public abstract Server getOutputNode();
  public abstract void startAll() throws IOException;
  public abstract void stopAll();
  public abstract void cleanAll();
}
