package ch.epfl.pop

import java.util.Arrays

import akka.routing.Broadcast
import ch.epfl.pop.crypto.Hash
import ch.epfl.pop.json.{Key, MessageContent, MessageContentData, MessageErrorContent}
import ch.epfl.pop.crypto.Signature.verify
import ch.epfl.pop.json.JsonMessages.{BroadcastLaoMessageClient, CreateLaoMessageClient, UpdateLaoMessageClient}
import ch.epfl.pop.json.JsonUtils.ErrorCodes.InvalidData

object Validate {

  /**
   * Verify that the signature and the id of the message are correct
   * @param content the message
   * @return None if correct, an error otherwise
   */
  def validate(content: MessageContent): Option[MessageErrorContent] = {
    if(!verify(content.encodedData, content.signature, content.sender)) {
      Some(MessageErrorContent(InvalidData.id, "Invalid signature"))
    }
    else {
      val id = Hash.computeMessageId(content.encodedData, content.signature)
      if(Arrays.equals(id, content.message_id)) {
        None
      }
      else {
        Some(MessageErrorContent(InvalidData.id, "Invalid message id"))
      }
    }
  }

  def validate(msg: CreateLaoMessageClient): Option[MessageErrorContent] = {
    val midLevelMsg = msg.params.message.get
    val lao = midLevelMsg.data
    val id = Hash.computeLAOId(lao.organizer, lao.creation, lao.name)

    if (!Arrays.equals(id, lao.id)) getError("Invalid LAO id.")
    else if (! (lao.creation > 0)) getError("Creation timestamp should be positive.")
    else if (! (lao.creation == lao.last_modified)) getError("Creation time should be the same as last modified.")
    else if (! Arrays.equals(midLevelMsg.sender, lao.organizer))
      getError("The sender of the message should be the same as the organizer of the LAO.")
    else None
  }

  def validate(msg: UpdateLaoMessageClient): Option[MessageErrorContent] = {
    val midLevelMsg = msg.params.message.get
    val laoUpdate = midLevelMsg.data

    if (!(laoUpdate.last_modified > laoUpdate.creation))
      getError("Last modified should be bigger than creation")
    else if (! Arrays.equals(midLevelMsg.sender, laoUpdate.organizer))
      getError("The sender of the message should be the same as the organizer of the LAO.")
    else None
  }

  def validate(msg: BroadcastLaoMessageClient, laoMod : MessageContentData): Option[MessageErrorContent] = {
    val midLevelMsg = msg.params.message.get
    val laoState = midLevelMsg.data

    val same = " should be the same in the state and the creation/modification message."
    val id = Hash.computeLAOId(laoState.organizer, laoState.creation, laoState.name)
    if (! Arrays.equals(id, laoState.id))
      getError("Invalid LAO id")
    else if (! (laoState.creation == laoMod.creation))
      getError("The creation timestamp" + same )
    else if (!(laoState.name == laoMod.name))
      getError("The name" + same)
    else if (!(laoState.last_modified == laoMod.last_modified))
      getError("The last modified timestamp" + same)
    else if (!Arrays.equals(laoState.organizer, laoMod.organizer))
      getError("The organizer" + same)
    else if (!(laoState.witnesses.length == laoMod.witnesses.length))
      getError("The number of witnesses" + same)
    else if (!compareWitnesses(laoState.witnesses, laoMod.witnesses))
      getError("Witnesses" + same)
    else if (! Arrays.equals(midLevelMsg.sender, laoState.organizer))
      getError("The sender of the message should be the same as the organizer of the LAO.")
    else None
  }

  private def compareWitnesses(w1: List[Key], w2: List[Key]): Boolean = {
    assert(w1.length == w2.length)
    w1.zip(w2).forall(p => Arrays.equals(p._1, p._2))
  }
  private def getError(error: String): Some[MessageErrorContent] = {
    Some(MessageErrorContent(InvalidData.id, error))
  }
}
