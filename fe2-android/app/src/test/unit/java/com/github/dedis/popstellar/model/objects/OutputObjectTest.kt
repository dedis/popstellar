package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.objects.digitalcash.ScriptOutputObject
import com.github.dedis.popstellar.testutils.Base64DataUtils
import org.junit.Assert
import org.junit.Test

class OutputObjectTest {
  @Test
  fun valueTest() {
    Assert.assertEquals(VALUE.toLong(), OUTPUT.value)
  }

  @Test
  fun scriptTest() {
    Assert.assertEquals(TYPE, OUTPUT.script.type)
    Assert.assertEquals(PUBKEYHASH, OUTPUT.pubKeyHash)
  }

  companion object {
    private val SENDER_KEY = Base64DataUtils.generateKeyPair()
    private val SENDER = SENDER_KEY.publicKey
    const val TYPE = "P2PKH"
    var PUBKEYHASH = SENDER.computeHash()
    private val SCRIPTTXOUT = ScriptOutputObject(TYPE, PUBKEYHASH)
    private const val VALUE = 32
    private val OUTPUT = OutputObject(VALUE.toLong(), SCRIPTTXOUT)
  }
}
