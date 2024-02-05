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
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.util.Objects
import timber.log.Timber

/**
 * Container of a high level message.
 *
 * It is encapsulated inside low level messages
 */
@Immutable
class MessageGeneral {
  val sender: PublicKey
  val dataEncoded: Base64URLData
  val data: Data
  val messageId: MessageID
  val signature: Signature

  var witnessSignatures: List<PublicKeySignaturePair> = ArrayList()
    get() = ArrayList(field)
    private set

  var isEmpty: Boolean = false

  constructor(
      sender: PublicKey,
      dataBuf: Base64URLData,
      data: Data,
      signature: Signature,
      messageID: MessageID,
      witnessSignatures: List<PublicKeySignaturePair>
  ) {
    this.sender = sender
    this.dataEncoded = dataBuf
    this.data = data
    this.messageId = messageID
    this.signature = signature
    this.witnessSignatures = ArrayList(witnessSignatures)
  }

  constructor(keyPair: KeyPair, data: Data?, gson: Gson) {
    requireNotNull(data)

    this.sender = keyPair.publicKey
    this.data = data

    val dataJson = gson.toJson(data, Data::class.java)
    Timber.tag(TAG).d(dataJson)

    this.dataEncoded = Base64URLData(dataJson.toByteArray(StandardCharsets.UTF_8))
    this.signature = generateSignature(keyPair.privateKey)
    this.messageId = MessageID(dataEncoded, signature)
  }

  constructor(
      keyPair: KeyPair,
      data: Data,
      witnessSignatures: List<PublicKeySignaturePair>,
      gson: Gson
  ) : this(keyPair, data, gson) {
    this.witnessSignatures = witnessSignatures
  }

  private fun generateSignature(signer: PrivateKey): Signature {
    return try {
      signer.sign(dataEncoded)
    } catch (e: GeneralSecurityException) {
      Timber.tag(TAG).d(e, "Failed to generate signature")
      Signature("")
    }
  }

  fun verify(): Boolean {
    if (!sender.verify(signature, dataEncoded)) {
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

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as MessageGeneral
    return sender == that.sender &&
        dataEncoded == that.dataEncoded &&
        data == that.data &&
        messageId == that.messageId &&
        signature == that.signature &&
        witnessSignatures == that.witnessSignatures
  }

  override fun hashCode(): Int {
    return Objects.hash(sender, dataEncoded, data, messageId, signature, witnessSignatures)
  }

  override fun toString(): String {
    return "MessageGeneral{sender='$sender', data='$data', signature='$signature', messageId='$messageId', witnessSignatures='${
      witnessSignatures.toTypedArray().contentToString()
    }'}"
  }

  companion object {
    private val TAG = MessageGeneral::class.java.simpleName
    val EMPTY = Object()
  }
}
