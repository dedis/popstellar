package util.examples.socialMedia

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.socialMedia.AddReaction
import ch.epfl.pop.model.objects._
import spray.json._

object AddReactionExamples {

  final val NOT_STALE_TIMESTAMP: Timestamp = Timestamp(1577833201L)
  final val SENDER_ADDREACTION: PublicKey = AddChirpExamples.SENDER_ADDCHIRP

  val invalidTimestamp: Timestamp = Timestamp(0)
  val chirpId: Hash = Hash(Base64Data.encode("chirpId"))
  val validReaction: String = "👍"

  val workingAddReaction: AddReaction = AddReaction(validReaction, chirpId, NOT_STALE_TIMESTAMP)
  final val MESSAGE_ADDREACTION_WORKING: Message = new Message(
    Base64Data.encode(workingAddReaction.toJson.toString),
    SENDER_ADDREACTION,
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(workingAddReaction)
  )

  val wrongTimestampAddReaction: AddReaction = AddReaction(validReaction, chirpId, invalidTimestamp)
  final val MESSAGE_ADDREACTION_WRONG_TIMESTAMP: Message = new Message(
    Base64Data.encode(wrongTimestampAddReaction.toJson.toString),
    SENDER_ADDREACTION,
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(wrongTimestampAddReaction)
  )
}
