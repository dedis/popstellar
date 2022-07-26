package util.examples.socialMedia

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.socialMedia.DeleteReaction
import ch.epfl.pop.model.objects._
import spray.json._

object DeleteReactionExamples {

  final val NOT_STALE_TIMESTAMP = Timestamp(1577833201L)
  final val SENDER_DELETEREACTION: PublicKey = AddChirpExamples.SENDER_ADDCHIRP

  val invalidTimestamp: Timestamp = Timestamp(0)
  val reactionId: Hash = Hash(Base64Data.encode("reactionId"))

  val workingDeleteReaction: DeleteReaction = DeleteReaction(reactionId, NOT_STALE_TIMESTAMP)
  final val MESSAGE_DELETEREACTION_WORKING: Message = new Message(
    Base64Data.encode(workingDeleteReaction.toJson.toString),
    SENDER_DELETEREACTION,
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(workingDeleteReaction)
  )

  val wrongTimestampDeleteReaction: DeleteReaction = DeleteReaction(reactionId, invalidTimestamp)
  final val MESSAGE_DELETEREACTION_WRONG_TIMESTAMP: Message = new Message(
    Base64Data.encode(wrongTimestampDeleteReaction.toJson.toString),
    SENDER_DELETEREACTION,
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(wrongTimestampDeleteReaction)
  )
}
