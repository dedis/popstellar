package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.Copyable
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import java.time.Instant
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap

/** Class to model a message that needs to be signed by witnesses */
class WitnessMessage : Copyable<WitnessMessage> {
  /** Base 64 URL encoded ID of the message that we want to sign */
  val messageId: MessageID

  /** Title that will be displayed for the message */
  var title: String

  /** Description that will be displayed for the message */
  var description: String

  /** Timestamp of the creation of the witnessing message used to sort messages by most recent */
  val timestamp: Long

  /** Set of witnesses that have signed the message */
  val witnesses: MutableSet<PublicKey> = ConcurrentHashMap.newKeySet()

  /**
   * Constructor for a Witness Message
   *
   * @param messageId ID of the message to sign
   */
  constructor(messageId: MessageID) {
    this.messageId = messageId
    this.timestamp = Instant.now().epochSecond
    this.title = ""
    this.description = ""
  }

  constructor(witnessMessage: WitnessMessage) {
    messageId = witnessMessage.messageId
    witnesses.addAll(witnessMessage.witnesses)
    title = witnessMessage.title
    description = witnessMessage.description
    timestamp = witnessMessage.timestamp
  }

  /**
   * Method to add a new witness that have signed the message
   *
   * @param pk public key of the witness that have signed the message
   */
  fun addWitness(pk: PublicKey) {
    witnesses.add(pk)
  }

  override fun copy(): WitnessMessage {
    return WitnessMessage(this)
  }

  override fun toString(): String {
    return "WitnessMessage{messageId='$messageId', witnesses=$witnesses, title='$title', description='$description'}"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is WitnessMessage) {
      return false
    }
    return messageId == other.messageId && witnesses == other.witnesses
  }

  override fun hashCode(): Int {
    return Objects.hash(messageId, witnesses)
  }
}
