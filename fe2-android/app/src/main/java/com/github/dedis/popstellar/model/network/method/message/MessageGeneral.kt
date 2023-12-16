package com.github.dedis.popstellar.model.network.method.message

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.model.objects.security.KeyPair
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PrivateKey
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.security.Signature
import com.google.gson.Gson
import timber.log.Timber
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.util.Arrays
import java.util.Objects

/**
 * Container of a high level message.
 *
 *
 * It is encapsulated inside low level messages
 */
@Immutable
class MessageGeneral {
    @JvmField
    val sender: PublicKey?
    val dataEncoded: Base64URLData?

    @JvmField
    val data: Data?

    @JvmField
    val messageId: MessageID?
    var signature: Signature? = null
        private set
    private var witnessSignatures: List<PublicKeySignaturePair> = ArrayList()

    constructor(
            sender: PublicKey?,
            dataBuf: Base64URLData?,
            data: Data?,
            signature: Signature?,
            messageID: MessageID?,
            witnessSignatures: List<PublicKeySignaturePair>?) {
        this.sender = sender
        dataEncoded = dataBuf
        this.data = data
        messageId = messageID
        this.signature = signature
        this.witnessSignatures = witnessSignatures?.let { ArrayList(it) }!!
    }

    constructor(keyPair: KeyPair, data: Data?, gson: Gson) {
        sender = keyPair.publicKey
        this.data = data
        val dataJson = gson.toJson(data, Data::class.java)
        Timber.tag(TAG).d(dataJson)
        dataEncoded = Base64URLData(dataJson.toByteArray(StandardCharsets.UTF_8))
        generateSignature(keyPair.privateKey)
        messageId = MessageID(dataEncoded, signature)
    }

    constructor(
            keyPair: KeyPair, data: Data?, witnessSignatures: List<PublicKeySignaturePair>, gson: Gson) : this(keyPair, data, gson) {
        this.witnessSignatures = witnessSignatures
    }

    private fun generateSignature(signer: PrivateKey) {
        try {
            signature = signer.sign(dataEncoded)
        } catch (e: GeneralSecurityException) {
            Timber.tag(TAG).d(e, "failed to generate signature")
        }
    }

    fun getWitnessSignatures(): List<PublicKeySignaturePair> {
        return ArrayList(witnessSignatures)
    }

    fun verify(): Boolean {
        if (!sender!!.verify(signature, dataEncoded)) {
            return false
        }
        if (data is WitnessMessageSignature) {
            val witness = data
            val witnessSignature = witness.signature
            val messageID = witness.messageId
            return sender.verify(witnessSignature, messageID)
        }
        return true
    }

    val isEmpty: Boolean
        get() = this == EMPTY

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as MessageGeneral
        return sender == that.sender && dataEncoded == that.dataEncoded && data == that.data && messageId == that.messageId && signature == that.signature && witnessSignatures == that.witnessSignatures
    }

    override fun hashCode(): Int {
        return Objects.hash(sender, dataEncoded, data, messageId, signature, witnessSignatures)
    }

    override fun toString(): String {
        return ("MessageGeneral{"
                + "sender='"
                + sender
                + '\''
                + ", data='"
                + data
                + "', signature='"
                + signature
                + '\''
                + ", messageId='"
                + messageId
                + '\''
                + ", witnessSignatures='"
                + Arrays.toString(witnessSignatures.toTypedArray())
                + "'}")
    }

    companion object {
        private val TAG = MessageGeneral::class.java.simpleName
        private val EMPTY = MessageGeneral(null, null, null, null, null, ArrayList())

        @JvmStatic
        fun emptyMessage(): MessageGeneral {
            return EMPTY
        }
    }
}