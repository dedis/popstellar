package com.github.dedis.popstellar.repository

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.federation.Challenge
import com.github.dedis.popstellar.model.network.method.message.data.federation.FederationResult
import com.github.dedis.popstellar.model.network.method.message.data.federation.TokensExchange
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.utility.handler.data.HandlerContext
import com.github.dedis.popstellar.utility.handler.data.LinkedOrganizationsHandler
import com.google.gson.Gson
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
    fun handleChallengeAndRepoTest() {
        val mockLaoRepo = Mockito.mock(LAORepository::class.java)
        val mockContext = Mockito.mock(HandlerContext::class.java)
        val mockRollCallRepo = Mockito.mock(RollCallRepository::class.java)
        REPO.flush()
        val handler = LinkedOrganizationsHandler(mockLaoRepo, REPO, mockRollCallRepo)
        handler.handleChallenge(mockContext, CHALLENGE)
        Assert.assertEquals(CHALLENGE, REPO.getChallenge())
    }

    @Test
    fun repoLinkedLaoTest() {
        Assert.assertEquals(mutableMapOf<String, Array<String>>(), REPO.getLinkedLaos(LAO_ID))

        REPO.addLinkedLao(LAO_ID, LAO_ID_2, TOKENS_ARRAY_1)
        REPO.addLinkedLao(LAO_ID_3, LAO_ID_2, TOKENS_ARRAY_2)
        REPO.updateAndNotifyLinkedLao(LAO_ID_4, LAO_ID_3, TOKENS_ARRAY_2, "rollcall")
        REPO.addLinkedLao(LAO_ID, LAO_ID_3, TOKENS_ARRAY_2)

        Assert.assertTrue(REPO.getLinkedLaos(LAO_ID).keys.contains(LAO_ID_2))
        Assert.assertTrue(REPO.getLinkedLaos(LAO_ID).keys.contains(LAO_ID_3))
        Assert.assertFalse(REPO.getLinkedLaos(LAO_ID).keys.contains(LAO_ID_4))
        Assert.assertTrue(REPO.getLinkedLaos(LAO_ID_3).keys.contains(LAO_ID_2))
        Assert.assertTrue(REPO.getLinkedLaos(LAO_ID_4).keys.contains(LAO_ID_3))
        Assert.assertFalse(REPO.getLinkedLaos(LAO_ID_4).keys.contains(LAO_ID))
        Assert.assertFalse(REPO.getLinkedLaos(LAO_ID_4).keys.contains(LAO_ID_2))
        Assert.assertFalse(REPO.getLinkedLaos(LAO_ID_4).keys.contains(LAO_ID_4))

        Assert.assertEquals(TOKENS_ARRAY_1, REPO.getLinkedLaos(LAO_ID)[LAO_ID_2])
        Assert.assertEquals(TOKENS_ARRAY_2, REPO.getLinkedLaos(LAO_ID)[LAO_ID_3])
        Assert.assertEquals(TOKENS_ARRAY_2, REPO.getLinkedLaos(LAO_ID_4)[LAO_ID_3])
    }

    @Test
    fun handleResultAndRepoTest() {
        val mockLaoRepo = Mockito.mock(LAORepository::class.java)
        val mockContext = Mockito.mock(HandlerContext::class.java)
        val mockChannel = Mockito.mock(Channel::class.java)
        Mockito.`when`(mockContext.channel).thenReturn(mockChannel)
        Mockito.`when`(mockChannel.extractLaoId()).thenReturn(LAO_ID_5)
        val mockRollCallRepo = Mockito.mock(RollCallRepository::class.java)
        REPO.flush()
        REPO.otherLaoId = LAO_ID_4
        REPO.updateChallenge(CHALLENGE)
        val handler = LinkedOrganizationsHandler(mockLaoRepo, REPO, mockRollCallRepo)
        Assert.assertThrows(NullPointerException::class.java) {
            handler.handleResult(mockContext, RESULT_SUCCESS)
        }
        Assert.assertEquals(mutableSetOf(LAO_ID_4), REPO.getLinkedLaos(LAO_ID_5).keys)
        Assert.assertTrue(REPO.getLinkedLaos(LAO_ID_5)[LAO_ID_4]!!.isEmpty())
    }

    @Test
    fun handleResultAndRepoTest2() {
        val mockLaoRepo = Mockito.mock(LAORepository::class.java)
        val mockContext = Mockito.mock(HandlerContext::class.java)
        val mockChannel = Mockito.mock(Channel::class.java)
        Mockito.`when`(mockContext.channel).thenReturn(mockChannel)
        Mockito.`when`(mockChannel.extractLaoId()).thenReturn(LAO_ID_7)
        val mockRollCallRepo = Mockito.mock(RollCallRepository::class.java)
        REPO.flush()
        REPO.otherLaoId = LAO_ID_4
        REPO.updateChallenge(CHALLENGE)
        val handler = LinkedOrganizationsHandler(mockLaoRepo, REPO, mockRollCallRepo)
        handler.handleResult(mockContext, RESULT_FAILURE)
        Assert.assertEquals(mutableSetOf<String>(), REPO.getLinkedLaos(LAO_ID_7).keys)

        REPO.otherLaoId = null
        handler.handleResult(mockContext, RESULT_SUCCESS)
        Assert.assertEquals(mutableSetOf<String>(), REPO.getLinkedLaos(LAO_ID_7).keys)
        REPO.flush()
        REPO.otherLaoId = LAO_ID_4
        handler.handleResult(mockContext, RESULT_SUCCESS)
        Assert.assertEquals(mutableSetOf<String>(), REPO.getLinkedLaos(LAO_ID_7).keys)
    }

    @Test
    fun handleTokensExchangeAndRepoTest() {
        val mockLaoRepo = Mockito.mock(LAORepository::class.java)
        val mockContext = Mockito.mock(HandlerContext::class.java)
        val mockChannel = Mockito.mock(Channel::class.java)
        Mockito.`when`(mockContext.channel).thenReturn(mockChannel)
        Mockito.`when`(mockChannel.extractLaoId()).thenReturn(LAO_ID_6)
        val mockRollCallRepo = Mockito.mock(RollCallRepository::class.java)
        REPO.flush()
        REPO.otherLaoId = LAO_ID_4
        REPO.updateChallenge(CHALLENGE)
        val handler = LinkedOrganizationsHandler(mockLaoRepo, REPO, mockRollCallRepo)
        Assert.assertThrows(NullPointerException::class.java) {
            handler.handleTokensExchange(mockContext, TOKENS_EXCHANGE)
        }
        Assert.assertEquals(mutableSetOf(LAO_ID_3), REPO.getLinkedLaos(LAO_ID_6).keys)
        Assert.assertEquals(TOKENS_ARRAY_1, REPO.getLinkedLaos(LAO_ID_6)[LAO_ID_3])
    }

    companion object {
        private val ORGANIZER = Base64DataUtils.generatePublicKey()
        private val CREATION = Instant.now().epochSecond
        private const val NAME = "Lao name"
        private val LAO_ID = Lao.generateLaoId(ORGANIZER, CREATION, NAME)
        private val LAO_ID_2 = "id2"
        private val LAO_ID_3 = "id3"
        private val LAO_ID_4 = "id4"
        private val LAO_ID_5 = "id5"
        private val LAO_ID_6 = "id6"
        private val LAO_ID_7 = "id7"
        private val TIMESTAMP = Instant.now().epochSecond
        private const val SERVER_ADDRESS = "wss://1.1.1.1:9000/client"
        private const val CHALLENGE_VALUE = "1feb2a2c7c739ea25f2568d056cc82d11be65d361511872cd35e4abd1a20f3d4"
        private val TOKENS_ARRAY_1 = arrayOf("token1", "token2")
        private val TOKENS_ARRAY_2 = arrayOf("token7", "token8", "token9")
        private val CHALLENGE = Challenge(CHALLENGE_VALUE, TIMESTAMP)
        private val MG_CHALLENGE = MessageGeneral(Base64DataUtils.generateKeyPair(), CHALLENGE, Gson())
        private val RESULT_SUCCESS = FederationResult("success", publicKey = "PK", challenge = MG_CHALLENGE)
        private val RESULT_FAILURE = FederationResult("failure", reason = "fail", challenge = MG_CHALLENGE)
        private val TOKENS_EXCHANGE = TokensExchange(LAO_ID_3, "rollCall", TOKENS_ARRAY_1, TIMESTAMP)
        private val REPO = LinkedOrganizationsRepository()
    }
}