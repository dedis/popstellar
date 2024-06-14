package com.github.dedis.popstellar.model.network.method.message.data.federation

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.google.gson.Gson
import org.junit.Assert
import org.junit.Test
import java.time.Instant

class FederationInitTest {
    @Test
    fun initLaoIdTest() {
        Assert.assertEquals(LAO_ID, INIT.laoId)
    }

    @Test
    fun initServerAddressTest() {
        Assert.assertEquals(SERVER_ADDRESS, INIT.serverAddress)
    }

    @Test
    fun initPublicKeyTest() {
        Assert.assertEquals(ORGANIZER.encoded, INIT.publicKey)
    }

    @Test
    fun initChallengeTest() {
        Assert.assertEquals(MG_CHALLENGE.toString(), INIT.challenge.toString())
    }

    @Test
    fun challengeObjectTest() {
        Assert.assertEquals(Objects.FEDERATION.`object`, INIT.`object`)
    }

    @Test
    fun challengeActionTest() {
        Assert.assertEquals(Action.INIT.action, INIT.action)
    }

    @Test
    fun equalsTest() {
        val init2 = FederationInit(LAO_ID, SERVER_ADDRESS, ORGANIZER.encoded, MG_CHALLENGE)
        Assert.assertEquals(INIT, init2)
        Assert.assertEquals(INIT, INIT)
        Assert.assertEquals(INIT.hashCode().toLong(), init2.hashCode().toLong())

        val challenge2 = Challenge("aaa", TIMESTAMP)
        val mg = MessageGeneral(Base64DataUtils.generateKeyPair(), challenge2, Gson())
        Assert.assertNotEquals(INIT, FederationInit(LAO_ID, SERVER_ADDRESS, ORGANIZER.encoded, mg))
        Assert.assertNotEquals(INIT, FederationInit("bbb", SERVER_ADDRESS, ORGANIZER.encoded, MG_CHALLENGE))
        Assert.assertNotEquals(INIT, null)
    }

    @Test
    fun toStringTest() {
        Assert.assertEquals(
                "FederationInit{lao_id='$LAO_ID', server_address='$SERVER_ADDRESS'," +
                        "public_key='${ORGANIZER.encoded}', challenge='$MG_CHALLENGE'}",
                INIT.toString()
        )
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
        private val MG_CHALLENGE = MessageGeneral(Base64DataUtils.generateKeyPair(), CHALLENGE, Gson())
        private val INIT = FederationInit(LAO_ID, SERVER_ADDRESS, ORGANIZER.encoded, MG_CHALLENGE)
    }
}