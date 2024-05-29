package com.github.dedis.popstellar.model.qrcode

import com.github.dedis.popstellar.model.network.method.message.data.federation.Challenge
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.security.KeyPair
import com.github.dedis.popstellar.testutils.Base64DataUtils
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class FederationDetailsTest {

    @Test
    fun extractLAOID() {
        Assert.assertEquals(LAO_ID, FEDERATION_DETAILS1.laoId)
        Assert.assertEquals(LAO_ID, FEDERATION_DETAILS2.laoId)
    }

    @Test
    fun extractServerAddress() {
        Assert.assertEquals(ADDRESS, FEDERATION_DETAILS1.serverAddress)
        Assert.assertEquals(ADDRESS, FEDERATION_DETAILS2.serverAddress)
    }

    @Test
    fun extractKey() {
        Assert.assertEquals(SENDER_KEY.publicKey.encoded, FEDERATION_DETAILS1.publicKey)
        Assert.assertEquals(SENDER_KEY.publicKey.encoded, FEDERATION_DETAILS2.publicKey)
    }

    @Test
    fun extractChallenge() {
        Assert.assertNull(FEDERATION_DETAILS1.challenge)
        Assert.assertEquals(CHALLENGE, FEDERATION_DETAILS2.challenge)
    }

    companion object {
        private val CREATION_TIME = Instant.now().epochSecond
        private const val LAO_NAME = "laoName"
        private val SENDER_KEY: KeyPair = Base64DataUtils.generatePoPToken()
        private val SENDER = SENDER_KEY.publicKey
        private val LAO_ID = Lao.generateLaoId(SENDER, CREATION_TIME, LAO_NAME)
        private const val ADDRESS = "localhost:9100"
        private const val CHALLENGE_VALUE =
                "frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0iumu_5NwAqXwGA"
        private val CHALLENGE = Challenge(CHALLENGE_VALUE, CREATION_TIME)

        private val FEDERATION_DETAILS1 = FederationDetails(LAO_ID, ADDRESS, SENDER_KEY.publicKey.encoded)
        private val FEDERATION_DETAILS2 = FederationDetails(LAO_ID, ADDRESS, SENDER_KEY.publicKey.encoded, CHALLENGE)
    }
}