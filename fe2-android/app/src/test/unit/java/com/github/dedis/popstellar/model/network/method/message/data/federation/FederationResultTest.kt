package com.github.dedis.popstellar.model.network.method.message.data.federation

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.google.gson.Gson
import org.junit.Assert
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class FederationResultTest {

    @Test
    fun resultStatusTest() {
        Assert.assertEquals(SUCCESS, RESULT_SUCCESS.status)
        Assert.assertEquals(FAILURE, RESULT_FAILURE.status)
    }

    @Test
    fun resultReasonTest() {
        Assert.assertNull(RESULT_SUCCESS.reason)
        Assert.assertEquals(REASON, RESULT_FAILURE.reason)
    }

    @Test
    fun resultPublicKeyTest() {
        Assert.assertEquals(PK.encoded, RESULT_SUCCESS.publicKey)
        Assert.assertNull(RESULT_FAILURE.publicKey)
    }

    @Test
    fun resultChallengeTest() {
        Assert.assertEquals(MG_CHALLENGE, RESULT_SUCCESS.challenge)
        Assert.assertEquals(MG_CHALLENGE, RESULT_FAILURE.challenge)
    }

    @Test
    fun resultObjectTest() {
        Assert.assertEquals(Objects.FEDERATION.`object`, RESULT_SUCCESS.`object`)
        Assert.assertEquals(Objects.FEDERATION.`object`, RESULT_FAILURE.`object`)
    }

    @Test
    fun resultActionTest() {
        Assert.assertEquals(Action.RESULT.action, RESULT_SUCCESS.action)
        Assert.assertEquals(Action.RESULT.action, RESULT_FAILURE.action)
    }

    @Test
    fun resultEqualsTest() {
        val result2 = FederationResult(SUCCESS, publicKey = PK.encoded, challenge = MG_CHALLENGE)
        val result3 = FederationResult(FAILURE, reason = REASON, challenge = MG_CHALLENGE)
        val result4 = FederationResult(FAILURE, reason = "reason2", challenge = MG_CHALLENGE)
        Assert.assertEquals(RESULT_SUCCESS, result2)
        Assert.assertEquals(RESULT_SUCCESS, RESULT_SUCCESS)
        Assert.assertEquals(RESULT_SUCCESS.hashCode().toLong(), result2.hashCode().toLong())
        Assert.assertEquals(RESULT_FAILURE, result3)
        Assert.assertEquals(RESULT_FAILURE, RESULT_FAILURE)
        Assert.assertEquals(RESULT_FAILURE.hashCode().toLong(), result3.hashCode().toLong())

        Assert.assertNotEquals(RESULT_SUCCESS, RESULT_FAILURE)
        Assert.assertNotEquals(RESULT_SUCCESS, result3)
        Assert.assertNotEquals(RESULT_SUCCESS, result4)
        Assert.assertNotEquals(RESULT_FAILURE, result2)
        Assert.assertNotEquals(RESULT_FAILURE, result4)
        Assert.assertNotEquals(RESULT_SUCCESS, null)
        Assert.assertNotEquals(RESULT_FAILURE, null)
    }

    @Test
    fun resultToStringTest() {
        Assert.assertEquals(
                "FederationResult{status='$SUCCESS', public_key='${PK.encoded}', challenge='$MG_CHALLENGE'}",
                RESULT_SUCCESS.toString()
        )
        Assert.assertEquals(
                "FederationResult{status='$FAILURE', reason='$REASON', challenge='$MG_CHALLENGE'}",
                RESULT_FAILURE.toString()
        )
    }

    @Test
    fun invalidMessageTypeTest() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            FederationResult("invalid", challenge = MG_CHALLENGE)
        }
        assert(exception.message == "Status must be either 'failure' or 'success'.")
    }

    @Test
    fun invalidSuccessTest1() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            FederationResult(SUCCESS, challenge = MG_CHALLENGE)
        }
        assert(exception.message == "Public key must be provided for success status.")
    }

    @Test
    fun invalidSuccessTest2() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            FederationResult(SUCCESS, publicKey = PK.encoded, reason = "reason", challenge = MG_CHALLENGE)
        }
        assert(exception.message == "Reason must be null for success status.")
    }

    @Test
    fun invalidFailureTest1() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            FederationResult(FAILURE, challenge = MG_CHALLENGE)
        }
        assert(exception.message == "Reason must be provided for failure status.")
    }

    @Test
    fun invalidFailureTest2() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            FederationResult(FAILURE, publicKey = PK.encoded, reason = "reason", challenge = MG_CHALLENGE)
        }
        assert(exception.message == "Public key must be null for failure status.")
    }

    companion object {
        private val KEY_PAIR = Base64DataUtils.generateKeyPair()
        private val PK = KEY_PAIR.publicKey
        private val TIMESTAMP = Instant.now().epochSecond
        private const val CHALLENGE_VALUE = "1feb2a2c7c739ea25f2568d056cc82d11be65d361511872cd35e4abd1a20f3d4"
        private val CHALLENGE = Challenge(CHALLENGE_VALUE, TIMESTAMP)
        private val MG_CHALLENGE = MessageGeneral(Base64DataUtils.generateKeyPair(), CHALLENGE, Gson())
        private val SUCCESS = "success"
        private val FAILURE = "failure"
        private val REASON = "reason"
        private val RESULT_SUCCESS = FederationResult(SUCCESS, publicKey = PK.encoded, challenge = MG_CHALLENGE)
        private val RESULT_FAILURE = FederationResult(FAILURE, reason = REASON, challenge = MG_CHALLENGE)
    }
}
