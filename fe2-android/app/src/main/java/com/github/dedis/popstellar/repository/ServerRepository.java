package com.github.dedis.popstellar.repository;

import com.github.dedis.popstellar.model.objects.Server;

import java.util.*;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents the set of all 'LAO connected backends'. Greetings message handling should handle this
 * repository. Should be a global repository.
 */
@Singleton
public class ServerRepository {

  private Map<String, Server> serverByLaoId;

  @Inject
  public ServerRepository() {
    serverByLaoId = new HashMap<>();
  }

  /** Add a server to the repository */
  public void addServer(String laoId, Server server) {
    serverByLaoId.put(laoId, server);
  }

  /** Get the corresponding server to the given Lao Id (if present) */
  public Server getServerByLaoId(String laoId) {
    if (serverByLaoId.containsKey(laoId)) {
      return serverByLaoId.get(laoId);
    }
    throw new IllegalArgumentException(
        String.format("There is no backend associated with the LAO '%s'", laoId));
  }

  /** Returns the complete collection of servers */
  public Collection<Server> getAllServer() {
    return serverByLaoId.values();
  }
}
