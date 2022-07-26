package util.examples.socialMedia

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.socialMedia.AddChirp
import ch.epfl.pop.model.objects._
import spray.json._

object AddChirpExamples {

  final val NOT_STALE_TIMESTAMP = Timestamp(1577833201L)
  final val SENDER_ADDCHIRP: PublicKey = PublicKey(Base64Data("to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic="))

  val invalidTimestamp: Timestamp = Timestamp(0)
  val invalidTextLength: String = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
  val validText: String = "valid"

  val workingAddChirp: AddChirp = AddChirp(validText, None, NOT_STALE_TIMESTAMP)
  final val MESSAGE_ADDCHIRP_WORKING: Message = new Message(
    Base64Data.encode(workingAddChirp.toJson.toString),
    SENDER_ADDCHIRP,
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(workingAddChirp)
  )

  val wrongTimestampAddChirp: AddChirp = AddChirp(validText, None, invalidTimestamp)
  final val MESSAGE_ADDCHIRP_WRONG_TIMESTAMP: Message = new Message(
    Base64Data.encode(wrongTimestampAddChirp.toJson.toString),
    SENDER_ADDCHIRP,
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(wrongTimestampAddChirp)
  )

  val wrongTextAddChirp: AddChirp = AddChirp(invalidTextLength, None, NOT_STALE_TIMESTAMP)
  final val MESSAGE_ADDCHIRP_WRONG_TEXT: Message = new Message(
    Base64Data.encode(wrongTextAddChirp.toJson.toString),
    SENDER_ADDCHIRP,
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(wrongTextAddChirp)
  )

}
