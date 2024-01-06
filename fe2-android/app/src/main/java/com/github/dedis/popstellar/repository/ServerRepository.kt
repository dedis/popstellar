package com.github.dedis.popstellar.repository

import com.github.dedis.popstellar.model.objects.Server
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents the set of all 'LAO connected backends'. Greetings message handling should handle this
 * repository. Should be a global repository.
 */
@Singleton
class ServerRepository @Inject constructor() {
  private val serverByLaoId: MutableMap<String, Server>

  init {
    serverByLaoId = HashMap()
  }

  /** Add a server to the repository */
  fun addServer(laoId: String, server: Server) {
    serverByLaoId[laoId] = server
  }

  /** Get the corresponding server to the given Lao Id (if present) */
  fun getServerByLaoId(laoId: String): Server? {
    if (serverByLaoId.containsKey(laoId)) {
      return serverByLaoId[laoId]
    }
    throw IllegalArgumentException("There is no backend associated with the LAO '$laoId'")
  }

  val allServer: Collection<Server>
    /** Returns the complete collection of servers */
    get() = serverByLaoId.values
}
