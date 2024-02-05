package com.github.dedis.popstellar.model.objects

import org.junit.Assert
import org.junit.Test

class PeerAddressTest {
  @Test
  fun address() {
    Assert.assertEquals(ADDRESS_1, PEER_ADDRESS_1.address)
  }

  @Test
  fun testToString() {
    val addressTest = "{address:='$ADDRESS_1'}"
    Assert.assertEquals(addressTest, PEER_ADDRESS_1.toString())
  }

  @Test
  fun testEquals() {
    Assert.assertEquals(PEER_ADDRESS_1, PEER_ADDRESS_1)
    Assert.assertNotEquals(PEER_ADDRESS_1, PEER_ADDRESS_2)
    Assert.assertNotEquals(PEER_ADDRESS_1, null)
  }

  companion object {
    var ADDRESS_1 = "ws://122.0.2"
    var ADDRESS_2 = "ws://122.0.3"
    var PEER_ADDRESS_1 = PeerAddress(ADDRESS_1)
    var PEER_ADDRESS_2 = PeerAddress(ADDRESS_2)
  }
}
