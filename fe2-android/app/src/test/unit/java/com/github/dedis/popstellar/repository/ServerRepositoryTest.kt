package com.github.dedis.popstellar.repository

import com.github.dedis.popstellar.model.objects.Server
import com.github.dedis.popstellar.model.objects.security.PublicKey
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ServerRepositoryTest {
  @Before
  fun setUp() {
    serverRepository.addServer(LAO_1, SERVER_1)
    serverRepository.addServer(LAO_2, SERVER_2)
  }

  @Test
  fun serverByLaoId() {
    Assert.assertEquals(SERVER_1, serverRepository.getServerByLaoId(LAO_1))
  }

  @Test
  fun serverByLaoIdFailsWithEmptyRepo() {
    val emptyRepo = ServerRepository()
    Assert.assertThrows(IllegalArgumentException::class.java) { emptyRepo.getServerByLaoId(LAO_1) }
  }

  @Test
  fun allServerTest() {
    Assert.assertFalse(serverRepository.allServer.isEmpty())
  }

  companion object {
    var serverRepository = ServerRepository()
    var LAO_1 = "id_1"
    var LAO_2 = "id_2"
    private var KEY_1 = PublicKey("oOcKZjUeandJOFVgn-E6e-7QksviBBbHUPicdzUgIm8")
    private var KEY_2 = PublicKey("oOcKZjUeandJOFVgn-E6e-7QksviBBbHUPicdzUgIm9")
    private var ADDRESS_1 = "10.0.2.2:9000"
    private var ADDRESS_2 = "128.0.0.2:9445"
    var SERVER_1 = Server(ADDRESS_1, KEY_1)
    var SERVER_2 = Server(ADDRESS_2, KEY_2)
  }
}
