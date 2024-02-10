package com.github.dedis.popstellar.model.network.method.message.data.digitalcash

import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.security.Signature
import org.junit.Assert
import org.junit.Test

class ScriptInputTest {
  @Test
  fun testGetType() {
    Assert.assertEquals(TYPE, SCRIPTTXIN.type)
  }

  @Test
  fun testGetSig() {
    Assert.assertEquals(Signature(SIG), SCRIPTTXIN.sig)
  }

  @Test
  fun testGetPub_key_recipient() {
    Assert.assertEquals(PUBKEY, SCRIPTTXIN.pubkey.encoded)
  }

  @Test
  fun testTestEquals() {
    Assert.assertEquals(SCRIPTTXIN, ScriptInput(TYPE, PublicKey(PUBKEY), Signature(SIG)))

    val random = "BBBBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB="

    Assert.assertNotEquals(SCRIPTTXIN, ScriptInput(random, PublicKey(PUBKEY), Signature(SIG)))
    Assert.assertNotEquals(SCRIPTTXIN, ScriptInput(TYPE, PublicKey(random), Signature(SIG)))
    Assert.assertNotEquals(SCRIPTTXIN, ScriptInput(TYPE, PublicKey(PUBKEY), Signature(random)))
  }

  companion object {
    private const val TYPE = "P2PKH"
    private const val PUBKEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    private const val SIG = "CAFEBABE"
    private val SCRIPTTXIN = ScriptInput(TYPE, PublicKey(PUBKEY), Signature(SIG))
  }
}
