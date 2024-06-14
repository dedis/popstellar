package com.github.dedis.popstellar.model.network.method.message.data.federation

import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import org.junit.Assert
import org.junit.Test
import java.time.Instant

class ChallengeRequestTest {
    @Test
    fun challengeRequestTimestampTest() {
        Assert.assertEquals(TIMESTAMP, CHALLENGE_REQUEST.timestamp)
    }

    @Test
    fun challengeRequestObjectTest() {
        Assert.assertEquals(Objects.FEDERATION.`object`, CHALLENGE_REQUEST.`object`)
    }

    @Test
    fun challengeRequestActionTest() {
        Assert.assertEquals(Action.CHALLENGE_REQUEST.action, CHALLENGE_REQUEST.action)
    }

    @Test
    fun equalsTest() {
        val challengeRequest2 = ChallengeRequest(TIMESTAMP)
        Assert.assertEquals(CHALLENGE_REQUEST, challengeRequest2)
        Assert.assertEquals(CHALLENGE_REQUEST, CHALLENGE_REQUEST)
        Assert.assertEquals(CHALLENGE_REQUEST.hashCode().toLong(), challengeRequest2.hashCode().toLong())

        Assert.assertNotEquals(CHALLENGE_REQUEST, ChallengeRequest(TIMESTAMP + 1))
        Assert.assertNotEquals(CHALLENGE_REQUEST, null)
    }

    @Test
    fun toStringTest() {
        Assert.assertEquals(
                "ChallengeRequest{timestamp='$TIMESTAMP'}",
                CHALLENGE_REQUEST.toString()
        )
    }

    companion object {
        private val TIMESTAMP = Instant.now().epochSecond
        private val CHALLENGE_REQUEST = ChallengeRequest(TIMESTAMP)
    }
}