package com.github.dedis.popstellar.model.qrcode

import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.model.objects.security.KeyPair
import com.github.dedis.popstellar.testutils.Base64DataUtils
import java.time.Instant
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PoPCHAQRCodeTest {
  @Test
  fun extractClientId() {
    Assert.assertEquals(CLIENT_ID, POPCHA_QR_CODE.clientId)
  }

  @Test
  fun extractNonce() {
    Assert.assertEquals(NONCE, POPCHA_QR_CODE.nonce)
  }

  @Test
  fun extractState() {
    Assert.assertEquals(STATE, POPCHA_QR_CODE.state)
  }

  @Test
  fun extractResponseMode() {
    Assert.assertEquals(RESPONSE_MODE, POPCHA_QR_CODE.responseMode)
  }

  @Test
  fun extractHost() {
    Assert.assertEquals(ADDRESS, POPCHA_QR_CODE.host)
  }

  @Test
  fun invalidUrl() {
    val invalidUrl = "http:/random."
    Assert.assertThrows(IllegalArgumentException::class.java) { PoPCHAQRCode(invalidUrl, LAO_ID) }
  }

  @Test
  fun invalidResponseMode() {
    val invalidQueryUrl = createPoPCHAUrl(ADDRESS, "resp", CLIENT_ID, LAO_ID, NONCE, STATE)
    Assert.assertThrows(IllegalArgumentException::class.java) {
      PoPCHAQRCode(invalidQueryUrl, LAO_ID)
    }
  }

  @Test
  fun invalidLaoId() {
    val invalidLaoUrl =
      createPoPCHAUrl(ADDRESS, RESPONSE_MODE, CLIENT_ID, "lao_wrong", NONCE, STATE)
    Assert.assertThrows(IllegalArgumentException::class.java) {
      PoPCHAQRCode(invalidLaoUrl, LAO_ID)
    }
  }

  companion object {
    private val CREATION_TIME = Instant.now().epochSecond
    private const val LAO_NAME = "laoName"
    private val SENDER_KEY: KeyPair = Base64DataUtils.generatePoPToken()
    private val SENDER = SENDER_KEY.publicKey
    private val LAO_ID = generateLaoId(SENDER, CREATION_TIME, LAO_NAME)
    private const val ADDRESS = "localhost:9100"
    private const val RESPONSE_MODE = "query"
    private const val CLIENT_ID = "WAsabGuEe5m1KpqOZQKgmO7UShX84Jmd_eaenOZ32wU"
    private const val NONCE =
      "frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0iumu_5NwAqXwGA"
    private const val STATE =
      "m_9r5sPUD8NoRIdVVYFMyYCOb-8xh1d2q8l-pKDXO0sn9TWnR_2nmC8MfVj1COHZsh1rElqimOTLAp3CbhbYJQ"
    private val URL = createPoPCHAUrl(ADDRESS, RESPONSE_MODE, CLIENT_ID, LAO_ID, NONCE, STATE)
    private val POPCHA_QR_CODE = PoPCHAQRCode(URL, LAO_ID)

    private fun createPoPCHAUrl(
      address: String,
      responseMode: String,
      clientId: String,
      laoId: String,
      nonce: String,
      state: String
    ): String {
      return ("http://$address/authorize?response_mode=$responseMode&response_type=id_token&" +
        "client_id=$clientId&redirect_uri=http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+profile&" +
        "login_hint=$laoId&nonce=$nonce&state=$state")
    }
  }
}
