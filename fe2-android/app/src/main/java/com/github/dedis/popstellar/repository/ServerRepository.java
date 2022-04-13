package com.github.dedis.popstellar.repository;

import com.github.dedis.popstellar.model.objects.Server;
import java.util.Collection;
import java.util.HashMap;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents the set of all backends that are currently connected
 * to the app. Greetings message handling should handle this repository.
 * Should be a global repository.
 */
@Singleton
public class ServerRepository {

  private Map<String, Server> serverByURL;

  @Inject
  public ServerRepository(){
    serverByURL = new HashMap<>();
  }

  /**
   * Add a server to the repository
   */
  public void addServer(Server server){
    serverByURL.put(server.getServerAddress(), server);
  }

  /**
   * Get the corresponding server to the given URL (if present)
   */
  public Server getServerByURL(String address){
    if (serverByURL.containsKey(address)) {
      return serverByURL.get(address);
    } else {
      throw new IllegalArgumentException(String.format("There is no server with address '%s'", address));
    }
  }

  /**
   * Returns the complete collection of the server
   */
  public Collection<Server> getAllServer(){
    return serverByURL.values();
  }

}
