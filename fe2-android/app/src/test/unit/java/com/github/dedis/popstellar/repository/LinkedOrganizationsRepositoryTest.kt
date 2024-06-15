package com.github.dedis.popstellar.repository

import com.github.dedis.popstellar.model.network.method.message.data.federation.Challenge
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.utility.handler.data.HandlerContext
import com.github.dedis.popstellar.utility.handler.data.LinkedOrganizationsHandler
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import java.time.Instant

class LinkedOrganizationsRepositoryTest {
    @Test
    fun repoFlushTest() {
        REPO.flush()
        Assert.assertNull(REPO.otherLaoId)
        Assert.assertNull(REPO.otherServerAddr)
        Assert.assertNull(REPO.otherPublicKey)
        Assert.assertNull(REPO.getChallenge())
    }

    @Test
    fun repoChallengeTest() {
        REPO.flush()
        REPO.updateChallenge(CHALLENGE)
        Assert.assertEquals(CHALLENGE, REPO.getChallenge())
        REPO.flush()
        Assert.assertNull(REPO.getChallenge())
    }

    @Test
    fun repoValuesTest() {
        REPO.flush()
        REPO.otherLaoId = LAO_ID
        REPO.otherPublicKey = ORGANIZER.encoded
        REPO.otherServerAddr = SERVER_ADDRESS
        Assert.assertEquals(LAO_ID, REPO.otherLaoId)
        Assert.assertEquals(SERVER_ADDRESS, REPO.otherServerAddr)
        Assert.assertEquals(ORGANIZER.encoded, REPO.otherPublicKey)
        REPO.flush()
        Assert.assertNull(REPO.otherLaoId)
        Assert.assertNull(REPO.otherServerAddr)
        Assert.assertNull(REPO.otherPublicKey)
    }

    @Test
    fun repoCallbackTest() {
        var c = Challenge("test", 0L)
        var changed = false
        REPO.flush()
        REPO.setOnChallengeUpdatedCallback { challenge: Challenge ->
            c = challenge
            changed = !changed
        }
        REPO.updateChallenge(CHALLENGE)
        Assert.assertEquals(CHALLENGE, c)
        Assert.assertEquals(CHALLENGE, REPO.getChallenge())
        Assert.assertTrue(changed)
    }

    @Test
    fun handlerAndRepoTest() {
        val mockLaoRepo = Mockito.mock(LAORepository::class.java)
        val mockContext = Mockito.mock(HandlerContext::class.java)
        REPO.flush()
        val handler = LinkedOrganizationsHandler(mockLaoRepo, REPO)
        handler.handleChallenge(mockContext, CHALLENGE)
        Assert.assertEquals(CHALLENGE, REPO.getChallenge())
    }

    companion object {
        private val ORGANIZER = Base64DataUtils.generatePublicKey()
        private val CREATION = Instant.now().epochSecond
        private const val NAME = "Lao name"
        private val LAO_ID = Lao.generateLaoId(ORGANIZER, CREATION, NAME)
        private val TIMESTAMP = Instant.now().epochSecond
        private const val SERVER_ADDRESS = "wss://1.1.1.1:9000/client"
        private const val CHALLENGE_VALUE = "1feb2a2c7c739ea25f2568d056cc82d11be65d361511872cd35e4abd1a20f3d4"
        private val CHALLENGE = Challenge(CHALLENGE_VALUE, TIMESTAMP)
        private val REPO = LinkedOrganizationsRepository()
    }
}