package com.github.dedis.popstellar.model.network.method.message.data.federation

import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import org.junit.Assert
import org.junit.Test
import java.time.Instant

class ChallengeTest {
    @Test
    fun challengeValueTest() {
        Assert.assertEquals(CHALLENGE_VALUE, CHALLENGE.value)
    }

    @Test
    fun challengeValidUntilTest() {
        Assert.assertEquals(TIMESTAMP, CHALLENGE.validUntil)
    }

    @Test
    fun challengeLowValidUntilTest() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            Challenge(CHALLENGE_VALUE, -1).validUntil
        }
    }

    @Test
    fun challengeObjectTest() {
        Assert.assertEquals(Objects.FEDERATION.`object`, CHALLENGE.`object`)
    }

    @Test
    fun challengeActionTest() {
        Assert.assertEquals(Action.CHALLENGE.action, CHALLENGE.action)
    }

    @Test
    fun equalsTest() {
        val challenge2 = Challenge(CHALLENGE_VALUE, TIMESTAMP)
        Assert.assertEquals(CHALLENGE, challenge2)
        Assert.assertEquals(CHALLENGE, CHALLENGE)
        Assert.assertEquals(CHALLENGE.hashCode().toLong(), challenge2.hashCode().toLong())

        Assert.assertNotEquals(CHALLENGE, Challenge(CHALLENGE_VALUE, TIMESTAMP + 1))
        Assert.assertNotEquals(CHALLENGE, Challenge("YWFh", TIMESTAMP))
        Assert.assertNotEquals(CHALLENGE, Challenge("YmJi", TIMESTAMP - 1))
        Assert.assertNotEquals(CHALLENGE, null)
    }

    @Test
    fun toStringTest() {
        Assert.assertEquals(
                "Challenge{value='$CHALLENGE_VALUE', valid_until='$TIMESTAMP'}",
                CHALLENGE.toString()
        )
    }

    @Test
    fun testInvalidChallenge() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            Challenge("", TIMESTAMP)
        }
    }

    companion object {
        private val TIMESTAMP = Instant.now().epochSecond + 2000
        private const val CHALLENGE_VALUE = "1feb2a2c7c739ea25f2568d056cc82d11be65d361511872cd35e4abd1a20f3d4"
        private val CHALLENGE = Challenge(CHALLENGE_VALUE, TIMESTAMP)
    }
}