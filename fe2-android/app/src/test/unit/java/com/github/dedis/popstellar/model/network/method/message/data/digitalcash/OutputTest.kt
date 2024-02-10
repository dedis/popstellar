package com.github.dedis.popstellar.model.network.method.message.data.digitalcash

import org.junit.Assert
import org.junit.Test

class OutputTest {
  @Test
  fun testGetValue() {
    Assert.assertEquals(VALUE.toLong(), TXOUT.value)
  }

  @Test
  fun testGetScript() {
    Assert.assertEquals(SCRIPT_TX_OUT, TXOUT.script)
  }

  @Test
  fun testEquals() {
    Assert.assertEquals(TXOUT, Output(VALUE.toLong(), SCRIPT_TX_OUT))
    val random = "random"

    Assert.assertNotEquals(TXOUT, Output(4, SCRIPT_TX_OUT))
    Assert.assertNotEquals(TXOUT, Output(VALUE.toLong(), ScriptOutput(random, PUBKEYHASH)))
  }

  companion object {
    private const val VALUE = 32
    private const val TYPE = "P2PKH"
    private const val PUBKEYHASH = "2jmj7l5rSw0yVb-vlWAYkK-YBwk="
    private val SCRIPT_TX_OUT = ScriptOutput(TYPE, PUBKEYHASH)
    private val TXOUT = Output(VALUE.toLong(), SCRIPT_TX_OUT)
  }
}
