package com.github.dedis.popstellar.model.network.method.message

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.di.DataRegistryModuleHelper.buildRegistry
import com.github.dedis.popstellar.di.JsonModule.provideGson
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.model.objects.security.KeyPair
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.security.Signature
import com.github.dedis.popstellar.model.objects.security.privatekey.PlainPrivateKey
import net.i2p.crypto.eddsa.Utils
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException

@RunWith(AndroidJUnit4::class)
class MessageGeneralTest {
  @Test
  fun testConstructorWithData() {
    val msg = MessageGeneral(KEY_PAIR, DATA, GSON)
    MatcherAssert.assertThat(msg.data, CoreMatchers.`is`(DATA))
    MatcherAssert.assertThat(msg.sender, CoreMatchers.`is`(KEY_PAIR.publicKey))
    MatcherAssert.assertThat(msg.witnessSignatures, CoreMatchers.`is`(emptyList<Any>()))
  }

  @Test
  fun testConstructorWithDataAndWitnessSignatures() {
    val msg = MessageGeneral(KEY_PAIR, DATA, WITNESS_SIGNATURES, GSON)
    MatcherAssert.assertThat(msg.data, CoreMatchers.`is`(DATA))
    MatcherAssert.assertThat(msg.sender, CoreMatchers.`is`(KEY_PAIR.publicKey))
    MatcherAssert.assertThat(msg.witnessSignatures, CoreMatchers.`is`(WITNESS_SIGNATURES))
  }

  @Test
  @Throws(GeneralSecurityException::class)
  fun testValueGeneration() {
    val msg = MessageGeneral(KEY_PAIR, DATA, GSON)
    MatcherAssert.assertThat(
      msg.dataEncoded,
      CoreMatchers.`is`(
        Base64URLData(
          GSON.toJson(DATA, Data::class.java).toByteArray(
            StandardCharsets.UTF_8
          )
        )
      )
    )
    MatcherAssert.assertThat(
      msg.signature,
      CoreMatchers.`is`(KEY_PAIR.privateKey.sign(msg.dataEncoded))
    )
    MatcherAssert.assertThat(
      msg.messageId,
      CoreMatchers.`is`(MessageID(msg.dataEncoded, msg.signature))
    )
  }

  @Test
  fun testFixedValueGeneration() {
    val msg = MessageGeneral(KEY_PAIR, DATA, GSON)
    MatcherAssert.assertThat(msg.dataEncoded, CoreMatchers.`is`(DATA_ENCODED))
    MatcherAssert.assertThat(msg.signature, CoreMatchers.`is`(SIGNATURE))
    MatcherAssert.assertThat(msg.messageId, CoreMatchers.`is`(MESSAGE_ID))
  }

  @Test
  fun verifyWorksOnValidData() {
    val msg1 = MessageGeneral(KEY_PAIR, DATA, WITNESS_SIGNATURES, GSON)
    val msg2 = MessageGeneral(
      KEY_PAIR.publicKey, DATA_ENCODED, DATA, SIGNATURE, MESSAGE_ID, WITNESS_SIGNATURES
    )
    MatcherAssert.assertThat(msg1.verify(), CoreMatchers.`is`(true))
    MatcherAssert.assertThat(msg2.verify(), CoreMatchers.`is`(true))
  }

  @Test
  fun verifyFailsOnInvalidData() {
    val msg = MessageGeneral(
      KEY_PAIR.publicKey,
      DATA_ENCODED,
      DATA,
      Signature("UB6xpjpUGN5VtmWAw1T3npHxiZfKaXzx3ny5PXl_qF4"),
      MESSAGE_ID,
      WITNESS_SIGNATURES
    )
    MatcherAssert.assertThat(msg.verify(), CoreMatchers.`is`(false))
  }

  @Test
  fun toStringTest() {
    val msg = MessageGeneral(
      KEY_PAIR.publicKey, DATA_ENCODED, DATA, SIGNATURE, MESSAGE_ID, WITNESS_SIGNATURES
    )
    val expected = String.format(
      "MessageGeneral{sender='%s', data='%s', signature='%s', messageId='%s', "
          + "witnessSignatures='%s'}",
      KEY_PAIR.publicKey.toString(), DATA, SIGNATURE, MESSAGE_ID, WITNESS_SIGNATURES
    )
    Assert.assertEquals(expected, msg.toString())
  }

  companion object {
    private val GSON = provideGson(buildRegistry())
    private val ORGANIZER = PublicKey("Z3DYtBxooGs6KxOAqCWD3ihR8M6ZPBjAmWp_w5VBaws=")
    private const val LAO_CREATION: Long = 1623825071
    private const val LAO_NAME = "LAO"
    private val DATA = CreateLao(
      generateLaoId(ORGANIZER, LAO_CREATION, LAO_NAME),
      LAO_NAME,
      LAO_CREATION,
      ORGANIZER,
      ArrayList()
    )
    private val DATA_ENCODED = Base64URLData(
      "eyJpZCI6Ik5PZjlHTGZKWTVjVVJkaUptaWxZcnNZT1phay1rXzd2MnV6NGxsQ1NFMU09IiwibmFtZSI6IkxBTyIsImNyZWF0aW9uIjoxNjIzODI1MDcxLCJvcmdhbml6ZXIiOiJaM0RZdEJ4b29HczZLeE9BcUNXRDNpaFI4TTZaUEJqQW1XcF93NVZCYXdzPSIsIndpdG5lc3NlcyI6W10sIm9iamVjdCI6ImxhbyIsImFjdGlvbiI6ImNyZWF0ZSJ9"
    )
    private val SIGNATURE = Signature(
      "OWT7z5-L25kCwFKvA0Rdz0HVXV57I8WZo183-skoEqfbojNLA78SEYhZjW6hT1lGJFGU2HefTkMBQzS49OkCDg=="
    )
    private val MESSAGE_ID = MessageID("3ZIQn9IRUQSBQChkb6gxRj_iYjXAiO-nx1KSBM6b79M=")
    private val KEY_PAIR = KeyPair(
      PlainPrivateKey(
        Utils.hexToBytes("3b28b4ab2fe355a13d7b24f90816ff0676f7978bf462fc84f1d5d948b119ec66")
      ),
      PublicKey("5c2zk_5uCrrNmdUhQAloCDqYJAC2rD4KHo9gGNFVS9c=")
    )
    private val WITNESS_SIGNATURES = listOf(
      PublicKeySignaturePair(
        PublicKey("FOosAAfgtHv0g_qZ5MyYTuvyiNXWtvrb0dMF4LY5O8M="),
        Signature(
          "7vcWfBmA0vU9_dkRxukAfMiWJRJOrERqLrIqFIpGZItTMSS0ZncurPamzd4seXZMR25yV9HIcyYeRJDZz4rUCGV5SnBaQ0k2SWs1UFpqbEhUR1pLV1RWalZWSmthVXB0YVd4WmNuTlpUMXBoYXkxclh6ZDJNblY2Tkd4c1ExTkZNVTA5SWl3aWJtRnRaU0k2SWt4QlR5SXNJbU55WldGMGFXOXVJam94TmpJek9ESTFNRGN4TENKdmNtZGhibWw2WlhJaU9pSmFNMFJaZEVKNGIyOUhjelpMZUU5QmNVTlhSRE5wYUZJNFRUWmFVRUpxUVcxWGNGOTNOVlpDWVhkelBTSXNJbmRwZEc1bGMzTmxjeUk2VzEwc0ltOWlhbVZqZENJNklteGhieUlzSW1GamRHbHZiaUk2SW1OeVpXRjBaU0o5"
        )
      )
    )
  }
}