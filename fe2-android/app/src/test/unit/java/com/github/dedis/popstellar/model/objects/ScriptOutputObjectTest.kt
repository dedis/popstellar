package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.objects.digitalcash.ScriptOutputObject
import com.github.dedis.popstellar.testutils.Base64DataUtils
import org.junit.Assert
import org.junit.Test

class ScriptOutputObjectTest {
  private val pubKeyHash = SENDER.computeHash()
  private val scriptTxOut = ScriptOutputObject(TYPE, pubKeyHash)

  @Test
  fun typeTest() {
    Assert.assertEquals(TYPE, scriptTxOut.type)
  }

  @Test
  fun publicKeyHashTest() {
    Assert.assertEquals(pubKeyHash, scriptTxOut.pubKeyHash)
  }

  companion object {
    private val SENDER_KEY = Base64DataUtils.generateKeyPair()
    private val SENDER = SENDER_KEY.publicKey
    private const val TYPE = "P2PKH"
  }
}
