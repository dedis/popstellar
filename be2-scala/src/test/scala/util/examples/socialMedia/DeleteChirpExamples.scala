package util.examples.socialMedia

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.socialMedia.DeleteChirp
import ch.epfl.pop.model.objects._
import spray.json._

object DeleteChirpExamples {

  final val NOT_STALE_TIMESTAMP = Timestamp(1577833201L)
  final val SENDER_DELETECHIRP: PublicKey = AddChirpExamples.SENDER_ADDCHIRP

  val invalidTimestamp: Timestamp = Timestamp(0)
  val chirpId: Hash = Hash(Base64Data.encode("chirpId"))

  val workingDeleteChirp: DeleteChirp = DeleteChirp(chirpId, NOT_STALE_TIMESTAMP)
  final val MESSAGE_DELETECHIRP_WORKING: Message = new Message(
    Base64Data.encode(workingDeleteChirp.toJson.toString),
    SENDER_DELETECHIRP,
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(workingDeleteChirp)
  )

  val wrongTimestampDeleteChirp: DeleteChirp = DeleteChirp(chirpId, invalidTimestamp)
  final val MESSAGE_DELETECHIRP_WRONG_TIMESTAMP: Message = new Message(
    Base64Data.encode(wrongTimestampDeleteChirp.toJson.toString),
    SENDER_DELETECHIRP,
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(wrongTimestampDeleteChirp)
  )

}
