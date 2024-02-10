package com.github.dedis.popstellar.model.network.method.message.data.digitalcash

import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.security.Signature
import org.junit.Assert
import org.junit.Test

class InputTest {
  @Test
  fun testGetTxOutHash() {
    Assert.assertEquals(Tx_OUT_HASH, TXIN.txOutHash)
  }

  @Test
  fun testGetTxOutIndex() {
    Assert.assertEquals(TX_OUT_INDEX.toLong(), TXIN.txOutIndex.toLong())
  }

  @Test
  fun testGetScript() {
    Assert.assertEquals(SCRIPTTXIN, TXIN.script)
  }

  @Test
  fun testTestEquals() {
    Assert.assertEquals(TXIN, Input(Tx_OUT_HASH, TX_OUT_INDEX, SCRIPTTXIN))

    val random = "random"

    Assert.assertNotEquals(TXIN, Input(random, TX_OUT_INDEX, SCRIPTTXIN))
    Assert.assertNotEquals(TXIN, Input(Tx_OUT_HASH, 4, SCRIPTTXIN))
    Assert.assertNotEquals(
      TXIN,
      Input(Tx_OUT_HASH, TX_OUT_INDEX, ScriptInput(random, PublicKey(PUBKEY), Signature(SIG)))
    )
  }

  companion object {
    private const val TX_OUT_INDEX = 0
    private const val Tx_OUT_HASH = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU="
    private const val TYPE = "P2PKH"
    private const val PUBKEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    private const val SIG = "CAFEBABE"
    private val SCRIPTTXIN = ScriptInput(TYPE, PublicKey(PUBKEY), Signature(SIG))
    private val TXIN = Input(Tx_OUT_HASH, TX_OUT_INDEX, SCRIPTTXIN)
  }
}
