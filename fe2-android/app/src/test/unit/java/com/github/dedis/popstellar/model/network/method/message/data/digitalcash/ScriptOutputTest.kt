package com.github.dedis.popstellar.model.network.method.message.data.digitalcash

import org.junit.Assert
import org.junit.Test

class ScriptOutputTest {
  @Test
  fun testGetType() {
    Assert.assertEquals(TYPE, SCRIPTTXOUT.type)
  }

  @Test
  fun testGetPub_key_hash() {
    Assert.assertEquals(PUBKEYHASH, SCRIPTTXOUT.pubKeyHash)
  }

  @Test
  fun testEquals() {
    Assert.assertEquals(SCRIPTTXOUT, ScriptOutput(TYPE, PUBKEYHASH))
    val random = "random"
    Assert.assertNotEquals(SCRIPTTXOUT, ScriptOutput(random, PUBKEYHASH))
    Assert.assertNotEquals(SCRIPTTXOUT, ScriptOutput(TYPE, random))
  }

  companion object {
    const val TYPE = "P2PKH"
    const val PUBKEYHASH = "2jmj7l5rSw0yVb-vlWAYkK-YBwk="
    val SCRIPTTXOUT = ScriptOutput(TYPE, PUBKEYHASH)
  }
}