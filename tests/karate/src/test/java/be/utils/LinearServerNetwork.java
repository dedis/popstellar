package be.utils;

import common.utils.MockClient;

import java.util.List;
import java.util.ArrayList;
import java.lang.Thread;
import java.io.IOException;

public class LinearServerNetwork {
  private final List<Server> servers;
  private final MockClient inputNodeClient;
  private final MockClient outputNodeClient;

  private LinearServerNetwork(List<Server> servers, MockClient inputNodeClient, MockClient outputNodeClient) {
    this.servers = servers;
    this.inputNodeClient = inputNodeClient;
    this.outputNodeClient = outputNodeClient;
  }

  public static LinearServerNetwork withOnlyGoServers(int numServers) throws IOException, InterruptedException {
    int availablePort = 8000;
    List<Server> servers = new ArrayList<>();

    for (int i = 0; i < numServers; i++) {
      servers.add(new GoServer("127.0.0.1", availablePort++, availablePort++, availablePort++, null, null));
    }

    for (int i = 0; i < numServers - 1; i++) {
      servers.get(i).pairWith(servers.get(i + 1));
    }

    for (Server server : servers) {
      server.start();
    }

    // Wait for all servers to start
    Thread.sleep(5000);

    MockClient inputNodeClient = new MockClient(servers.get(0).getWsClientURL());
    MockClient outputNodeClient = new MockClient(servers.get(numServers - 1).getWsClientURL());
    return new LinearServerNetwork(servers, inputNodeClient, outputNodeClient);
  }

  public List<Server> getServers() {
    return servers;
  }

  public MockClient getInputNodeClient() {
    return inputNodeClient;
  }

  public MockClient getOutputNodeClient() {
    return outputNodeClient;
  }

  public Server getInputNode() {
    return servers.get(0);
  }

  public Server getOutputNode() {
    return servers.get(servers.size() - 1);
  }

  public void stopAll() {
    servers.forEach(Server::stop);
  }

  public void cleanAll() {
    servers.forEach(Server::deleteDatabaseDir);
  }
}
