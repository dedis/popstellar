package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.objects.security.PublicKey
import org.junit.Assert
import org.junit.Test

class ServerTest {
  @Test
  fun publicKeyTest() {
    Assert.assertEquals(RANDOM_KEY, SERVER.publicKey)
  }

  @Test
  fun serverAddressTest() {
    Assert.assertEquals(RANDOM_ADDRESS, SERVER.serverAddress)
  }

  companion object {
    val RANDOM_KEY = PublicKey("oOcKZjUeandJOFVgn-E6e-7QksviBBbHUPicdzUgIm8")
    const val RANDOM_ADDRESS = "ws://10.0.2.2:9000/organizer/client"
    val SERVER = Server(RANDOM_ADDRESS, RANDOM_KEY)
  }
}
