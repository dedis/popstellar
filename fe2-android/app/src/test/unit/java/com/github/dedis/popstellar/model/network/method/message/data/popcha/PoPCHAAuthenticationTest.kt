package com.github.dedis.popstellar.model.network.method.message.data.popcha

import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import org.junit.Assert
import org.junit.Test

class PoPCHAAuthenticationTest {
  @Test
  fun clientId() {
    Assert.assertEquals(CLIENT_ID, POPCHA_AUTHENTICATION.clientId)
  }

  @Test
  fun nonce() {
    Assert.assertEquals(NONCE, POPCHA_AUTHENTICATION.nonce)
  }

  @Test
  fun identifier() {
    Assert.assertEquals(IDENTIFIER, POPCHA_AUTHENTICATION.identifier)
  }

  @Test
  fun identifierProof() {
    Assert.assertEquals(IDENTIFIER_PROOF, POPCHA_AUTHENTICATION.identifierProof)
  }

  @Test
  fun state() {
    Assert.assertEquals(STATE, POPCHA_AUTHENTICATION.state)
  }

  @Test
  fun responseMode() {
    Assert.assertEquals(RESPONSE_MODE, POPCHA_AUTHENTICATION.responseMode)
  }

  @Test
  fun popchaAddress() {
    Assert.assertEquals(ADDRESS, POPCHA_AUTHENTICATION.popchaAddress)
  }

  @Test
  fun `object`() {
    Assert.assertEquals(Objects.POPCHA.`object`, POPCHA_AUTHENTICATION.`object`)
  }

  @Test
  fun action() {
    Assert.assertEquals(Action.AUTH.action, POPCHA_AUTHENTICATION.action)
  }

  @Test
  fun testEquals() {
    Assert.assertEquals(POPCHA_AUTHENTICATION, POPCHA_AUTHENTICATION)
    Assert.assertNotEquals(null, POPCHA_AUTHENTICATION)
    val popCHAAuthentication1 =
      PoPCHAAuthentication(
        CLIENT_ID,
        NONCE,
        IDENTIFIER,
        IDENTIFIER_PROOF,
        ADDRESS,
        STATE,
        RESPONSE_MODE
      )
    Assert.assertEquals(POPCHA_AUTHENTICATION, popCHAAuthentication1)
  }

  @Test
  fun testHashCode() {
    Assert.assertEquals(
      java.util.Objects.hash(
          CLIENT_ID,
          NONCE,
          IDENTIFIER,
          IDENTIFIER_PROOF,
          STATE,
          RESPONSE_MODE,
          ADDRESS
        )
        .toLong(),
      POPCHA_AUTHENTICATION.hashCode().toLong()
    )
  }

  @Test
  fun testToString() {
    val expected =
      String.format(
        "PoPCHAAuthentication{clientId='%s', nonce='%s', identifier='%s', identifierProof='%s', state='%s', responseMode='%s', popchaAddress='%s'}",
        CLIENT_ID,
        NONCE,
        IDENTIFIER,
        IDENTIFIER_PROOF,
        STATE,
        RESPONSE_MODE,
        ADDRESS
      )
    Assert.assertEquals(expected, POPCHA_AUTHENTICATION.toString())
  }

  companion object {
    private val CLIENT_ID = hash("clientID")
    private val NONCE = hash("random")
    private val IDENTIFIER = Base64URLData("identifier")
    private val IDENTIFIER_PROOF = Base64URLData("identifier-proof")
    private val STATE: String? = null
    private const val RESPONSE_MODE = "id_token"
    private const val ADDRESS = "localhost:9100"
    private val POPCHA_AUTHENTICATION =
      PoPCHAAuthentication(
        CLIENT_ID,
        NONCE,
        IDENTIFIER,
        IDENTIFIER_PROOF,
        ADDRESS,
        STATE,
        RESPONSE_MODE
      )
  }
}
