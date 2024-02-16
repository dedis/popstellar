package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.objects.digitalcash.ScriptInputObject
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.security.Signature
import com.github.dedis.popstellar.testutils.Base64DataUtils
import org.junit.Assert
import org.junit.Test

class ScriptInputObjectTest {
  private var type: String = "P2PKH"
  private var pubKey: String = SENDER.encoded
  private var sig: String = SENDER_KEY.sign(SENDER).encoded
  private var scriptTxIn: ScriptInputObject =
    ScriptInputObject(type, PublicKey(pubKey), Signature(sig))

  @Test
  fun typeTest() {
    Assert.assertEquals(type, scriptTxIn.type)
  }

  @Test
  fun pubKeyTest() {
    Assert.assertEquals(pubKey, scriptTxIn.pubKey.encoded)
  }

  @Test
  fun sigTest() {
    Assert.assertEquals(sig, scriptTxIn.sig.encoded)
  }

  companion object {
    private val SENDER_KEY = Base64DataUtils.generateKeyPair()
    private val SENDER = SENDER_KEY.publicKey
  }
}
