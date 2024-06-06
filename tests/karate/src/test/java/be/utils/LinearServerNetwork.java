package be.utils;

import common.utils.MockClient;

import java.util.List;
import java.util.ArrayList;
import java.lang.Thread;
import java.io.IOException;

public class LinearServerNetwork {
  private final List<Server> servers;
  private static int nextAvailablePort = 8000;

  /**
   * Create a linear network of servers with the given servers.
   * @param servers the servers in the network. The first server is the input node and the last server is the output node. The servers should not be paired.
   */
  private LinearServerNetwork(List<Server> servers) {
    this.servers = List.copyOf(servers);

    for (int i = 0; i < servers.size() - 1; i++) {
      servers.get(i).pairWith(servers.get(i + 1));
    }
  }

  public static LinearServerNetwork withOnlyGoServers(int numServers) {
    List<Server> servers = new ArrayList<>();

    for (int i = 0; i < numServers; i++) {
      servers.add(new GoServer("127.0.0.1", nextAvailablePort++, nextAvailablePort++, nextAvailablePort++, null, null));
    }

    return new LinearServerNetwork(servers);
  }

  public static LinearServerNetwork withOnlyScalaServers(int numServers) throws IOException, InterruptedException {
    List<Server> servers = new ArrayList<>();

    for (int i = 0; i < numServers; i++) {
      servers.add(new ScalaServer("127.0.0.1", nextAvailablePort++, null, null));
    }

    return new LinearServerNetwork(servers);
  }

  public List<Server> getServers() {
    return List.copyOf(servers);
  }

  public MockClient createInputNodeClient() {
    return new MockClient(getInputNode().getWsClientURL());
  }

  public MockClient createOutputNodeClient() {
    return new MockClient(getOutputNode().getWsClientURL());
  }

  public Server getInputNode() {
    return servers.get(0);
  }

  public Server getOutputNode() {
    return servers.get(servers.size() - 1);
  }

  public void startAll() throws IOException {
    for (Server server : servers) {
      server.start();
    }
  }

  public void stopAll() {
    servers.forEach(Server::stop);
  }

  public void cleanAll() {
    servers.forEach(Server::deleteDatabaseDir);
  }
}
