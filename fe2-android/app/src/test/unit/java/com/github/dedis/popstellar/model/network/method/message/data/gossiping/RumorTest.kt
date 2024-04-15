package com.github.dedis.popstellar.model.network.method.message.data.gossiping

import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils.loadFile
import com.github.dedis.popstellar.model.network.JsonTestUtils.parse
import com.github.dedis.popstellar.model.network.JsonTestUtils.testData
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import com.google.gson.JsonParseException
import okio.ByteString.Companion.encode
import org.hamcrest.CoreMatchers
import org.hamcrest.EasyMock2Matchers.equalTo
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class RumorTest {

    private val validSenderId = Base64DataUtils.generatePublicKey().encoded
    private val invalidSenderId = "this is not base64"
    private val validRumorId = 12345
    private val invalidRumorId = -1
    private val validData = Base64URLData("Message data".toByteArray()).encoded
    private val validSignature = Base64URLData("signature".toByteArray()).encoded
    private val validMessageId = Base64URLData("messageId".toByteArray()).encoded

    private val validChannelId = Base64DataUtils.generateMessageID().encoded
    private val validChannelId2 = Base64DataUtils.generateMessageID().encoded

    private val validMessages = listOf(
        mapOf(validChannelId to listOf(mapOf("data" to validData, "sender" to validSenderId, "signature" to validSignature, "message_id" to validMessageId))),
        mapOf(validChannelId2 to listOf(mapOf("data" to validData, "sender" to validSenderId, "signature" to validSignature, "message_id" to validMessageId)))
    )
    private val differentValidMessages = listOf(
        mapOf(validChannelId to listOf(mapOf("data" to validData, "sender" to validSenderId, "signature" to validSignature, "message_id" to validMessageId))),
        mapOf(validChannelId2 to listOf(mapOf("data" to validData, "sender" to validSenderId, "signature" to validSignature, "message_id" to validMessageId))),
        mapOf(validChannelId to listOf(mapOf("data" to validData, "sender" to validSenderId, "signature" to validSignature, "message_id" to validMessageId)))
    )

    private val emptyMessagesList = emptyList<Map<String, List<Any>>>()
    private val invalidMessages = listOf(mapOf("channelId" to listOf(mapOf("data" to "Invalid message data", "sender" to Base64DataUtils.generatePublicKey(), "signature" to Base64DataUtils.generateSignature(), "message_id" to Base64DataUtils.generateMessageID())))    )

    private val rumor = Rumor(validSenderId, validRumorId, validMessages)
    private val rumorDifferentSenderId = Rumor(Base64DataUtils.generatePublicKey().encoded, validRumorId, validMessages)
    private val rumorDifferentRumorId = Rumor(validSenderId, validRumorId + 1, validMessages)
    private val rumorDifferentMessages = Rumor(validSenderId, validRumorId, differentValidMessages)


    @Test
    fun constructorSucceedsWithValidData() {
        val rumor = Rumor(validSenderId, validRumorId, validMessages)
        Assert.assertNotNull(rumor)
    }

    @Test(expected = IllegalArgumentException::class)
    fun constructorFailsWhenSenderIdNotBase64() {
        Rumor(invalidSenderId, validRumorId, validMessages)
    }

    @Test(expected = IllegalArgumentException::class)
    fun constructorFailsWhenRumorIdNegative() {
        Rumor(validSenderId, invalidRumorId, validMessages)
    }

    @Test(expected = IllegalArgumentException::class)
    fun constructorFailsWithEmptyMessagesList() {
        Rumor(validSenderId, validRumorId, emptyMessagesList)
    }

    @Test(expected = IllegalArgumentException::class)
    fun constructorFailsWithInvalidMessages() {
        Rumor(validSenderId, validRumorId, invalidMessages)
    }

    @Test
    fun rumorGetterReturnsCorrectSenderId() {
        val rumor = Rumor(validSenderId, validRumorId, validMessages)
        MatcherAssert.assertThat(rumor.senderId, CoreMatchers.`is`(validSenderId))
    }

    @Test
    fun rumorGetterReturnsCorrectRumorId() {
        val rumor = Rumor(validSenderId, validRumorId, validMessages)
        MatcherAssert.assertThat(rumor.rumorId, CoreMatchers.`is`(validRumorId))
    }

    @Test
    fun rumorGetterReturnsCorrectMessages() {
        val rumor = Rumor(validSenderId, validRumorId, validMessages)
        MatcherAssert.assertThat(rumor.messages, CoreMatchers.`is`(validMessages))
    }

    @Test
    fun equalsTest() {
        val rumor2 = Rumor(validSenderId, validRumorId, validMessages)
        Assert.assertEquals(rumor, rumor2)
        Assert.assertEquals(rumor.hashCode(), rumor2.hashCode())

        Assert.assertNotEquals(rumor, rumorDifferentRumorId)

        Assert.assertNotEquals(rumor, rumorDifferentMessages)

        Assert.assertNotEquals(rumor, rumorDifferentSenderId)

        Assert.assertNotEquals(rumor, null)
    }

    @Test
    fun toStringTest() {
        val expectedString = "Rumor(senderId='$validSenderId', rumorId=$validRumorId, messages=$validMessages)"
        assertEquals(expectedString, rumor.toString())
    }

    @Test
    fun jsonValidationTest() {
        val pathDir = "protocol/examples/query/rumor/"
        val jsonFiles = listOf(
            "rumor.json",
            "wrong_rumor_additional_params.json",
            "wrong_rumor_missing_channel.json",
            "wrong_rumor_missing_messages.json",
            "wrong_rumor_missing_rumor_id.json",
            "wrong_rumor_missing_sender_id.json"
        )

        val validJson = loadFile(pathDir + jsonFiles[1])
        parse(validJson)

        for (i in 1 until jsonFiles.size) {
            val invalidJson = loadFile(pathDir + jsonFiles[i])
            Assert.assertThrows(JsonParseException::class.java) { parse(invalidJson) }
        }
    }

}