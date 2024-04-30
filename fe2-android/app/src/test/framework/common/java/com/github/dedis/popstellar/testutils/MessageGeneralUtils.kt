package com.github.dedis.popstellar.testutils

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.PublicKeySignaturePair
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.security.Signature
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Random

/**
 * This class will hold utility functions that are used to create MessageGeneral objects
 */
class MessageGeneralUtils {

    companion object {
        private val SEED: Long = 2024
        private val RANDOM = Random(SEED)

        /**
         * generates a MessageGeneral object with valid data
         * number of witnesses will be random
         * @return a MessageGeneral object with valid data
         */
        private fun generateMessageGeneral() : MessageGeneral {
            val validData = object : Data {
                override val `object`: String
                    get() = "object"
                override val action: String
                    get() = "action"
            }
            val validPublicKey = Base64DataUtils.generatePublicKey()
            val validSignature = Base64DataUtils.generateSignature()
            val validMessageId = Base64DataUtils.generateMessageID()

            val witnesses = List(RANDOM.nextInt(10)) {
                PublicKeySignaturePair(Base64DataUtils.generatePublicKey(), Base64DataUtils.generateSignature())
            }

            return MessageGeneral(validPublicKey, Base64URLData("validData".toByteArray()), validData, validSignature, validMessageId, witnesses)
        }

        /**
         * generates a MessageGeneral object with valid data
         */
        fun generateListMessageGeneral(size: Int) : List<MessageGeneral> {
            return List(size) { generateMessageGeneral() }
        }

        fun getInvalidMessageGeneralOfEach(): List<MessageGeneral> {
            val invalidFields = listOf("data", "sender", "signature", "messageId")
            val invalidityType = listOf("nonBase64", "empty")

            return invalidFields.flatMap { field ->
                invalidityType.map { invalidity ->
                    createInvalidMessageGeneral(field, invalidity)
                }
            }
        }

        private fun createInvalidMessageGeneral(invalidField: String, invalidity: String): MessageGeneral {
            val validData = mock(Base64URLData::class.java)
            val validPublicKey = mock(PublicKey::class.java)
            val validSignature = mock(Signature::class.java)
            val validMessageId = mock(MessageID::class.java)

            `when`(validPublicKey.encoded).thenReturn("ValidBase64PublicKey")
            `when`(validSignature.encoded).thenReturn("ValidBase64Signature")
            `when`(validMessageId.encoded).thenReturn("ValidBase64MessageId")
            `when`(validData.encoded).thenReturn("Valid data encoded")

            val invalidValue = when (invalidity) {
                "nonBase64" -> "InvalidBase64==="
                "empty" -> ""
                else -> throw IllegalArgumentException("Invalid invalidity type: $invalidity")
            }

            when (invalidField) {
                "data" -> `when`(validData.encoded).thenReturn(invalidValue)
                "sender" -> `when`(validPublicKey.encoded).thenReturn(invalidValue)
                "signature" -> `when`(validSignature.encoded).thenReturn(invalidValue)
                "messageId" -> `when`(validMessageId.encoded).thenReturn(invalidValue)
            }

            return MessageGeneral(
                sender = validPublicKey,
                dataBuf = validData,
                data = object : Data {
                    override val `object`: String get() = "object"
                    override val action: String get() = "action"
                },
                signature = validSignature,
                messageID = validMessageId,
                witnessSignatures = listOf()
            )
        }
    }
}