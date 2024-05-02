package com.github.dedis.popstellar.model.network.method.message.data.gossiping

    import androidx.test.ext.junit.runners.AndroidJUnit4
    import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
    import com.github.dedis.popstellar.testutils.Base64DataUtils
    import com.github.dedis.popstellar.testutils.MessageGeneralUtils.Companion.generateListMessageGeneral
    import com.github.dedis.popstellar.testutils.MessageGeneralUtils.Companion.getInvalidMessageGeneralOfEach
    import org.hamcrest.CoreMatchers
    import org.hamcrest.MatcherAssert
    import org.junit.Assert
    import org.junit.Assert.assertThrows
    import org.junit.Test
    import org.junit.runner.RunWith
    import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class RumorTest {

    private val validSenderId = Base64DataUtils.generatePublicKey().encoded
    private val invalidSenderId = "this is not base64"
    private val validRumorId = 12345
    private val invalidRumorId = -1

    private val validChannelId = Base64DataUtils.generateMessageID().encoded
    private val validChannelId2 = Base64DataUtils.generateMessageID().encoded

    private val messagesGeneral1 = generateListMessageGeneral(10)
    private val messagesGeneral2 = generateListMessageGeneral(5)
    private val messagesGeneral3 = generateListMessageGeneral(3)

    private val validMessages = mapOf(validChannelId to messagesGeneral1, validChannelId2 to messagesGeneral2)
    private val differentValidMessages = mapOf(validChannelId to messagesGeneral3, validChannelId2 to messagesGeneral2)

    private val emptyMessagesList = mapOf<String, List<MessageGeneral>>()
    private val invalidMessages = getInvalidMessageGeneralOfEach()

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

    @Test
    fun constructorFailsWithInvalidMessages() {
        for (invalidMessage in invalidMessages) {
            val messages = mapOf(validChannelId to listOf(invalidMessage))
            assertThrows(IllegalArgumentException::class.java) {
                Rumor(validSenderId, validRumorId, messages)
            }
        }
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
        val notARumor = "Not a rumor"

        Assert.assertEquals(rumor, rumor2)
        Assert.assertEquals(rumor.hashCode(), rumor2.hashCode())

        Assert.assertNotEquals(rumor, rumorDifferentRumorId)

        Assert.assertNotEquals(rumor, rumorDifferentMessages)

        Assert.assertNotEquals(rumor, rumorDifferentSenderId)

        Assert.assertNotEquals(rumor, null)

        Assert.assertNotEquals(rumor, notARumor)
    }

    @Test
    fun toStringTest() {
        val expectedString = "Rumor(senderId='$validSenderId', rumorId=$validRumorId, messages=$validMessages)"
        assertEquals(expectedString, rumor.toString())
    }

    /* TODO : Can't seem to get this to work, will need to come back to it - Maxime Teuber @kaz-ookid 04/2024
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

        val validJson = loadFile(pathDir + jsonFiles[0])
        parse(validJson)

        for (i in 1 until jsonFiles.size) {
            val invalidJson = loadFile(pathDir + jsonFiles[i])
            Assert.assertThrows(JsonParseException::class.java) { parse(invalidJson) }
        }
    }
    */

}