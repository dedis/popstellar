package be.utils;

import common.utils.MockClient;

import java.util.List;

public abstract class ServerNetwork {
  public abstract List<Server> getServers();
  public abstract MockClient getInputNodeClient();
  public abstract MockClient getOutputNodeClient();
  public abstract Server getInputNode();
  public abstract Server getOutputNode();
  public abstract void stopAll();
  public abstract void cleanAll();
}
