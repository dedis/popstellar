package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.objects.digitalcash.ScriptInputObject
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.security.Signature
import com.github.dedis.popstellar.testutils.Base64DataUtils
import org.junit.Assert
import org.junit.Test

class InputObjectTest {
  private var type: String = "P2PKH"
  private var pubKey: String = SENDER.encoded
  private var sig: String = SENDER_KEY.sign(SENDER).encoded
  private var scriptTxIn: ScriptInputObject =
    ScriptInputObject(type, PublicKey(pubKey), Signature(sig))
  private var input: InputObject = InputObject(Tx_OUT_HASH, TX_OUT_INDEX, scriptTxIn)

  @Test
  fun txOutIndexTest() {
    Assert.assertEquals(TX_OUT_INDEX, input.txOutIndex)
  }

  @Test
  fun txOutHashTest() {
    Assert.assertEquals(Tx_OUT_HASH, input.txOutHash)
  }

  @Test
  fun scriptTest() {
    Assert.assertEquals(input.script.pubKey.encoded, pubKey)
    Assert.assertEquals(input.script.sig.encoded, sig)
    Assert.assertEquals(input.script.type, type)
  }

  companion object {
    private const val TX_OUT_INDEX = 0
    private const val Tx_OUT_HASH = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU="
    private val SENDER_KEY = Base64DataUtils.generateKeyPair()
    private val SENDER = SENDER_KEY.publicKey
  }
}
