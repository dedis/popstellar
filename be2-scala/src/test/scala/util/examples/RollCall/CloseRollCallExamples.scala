package util.examples.RollCall

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.rollCall._
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.graph.validators.RollCallValidator.EVENT_HASH_PREFIX
import spray.json._
import util.examples.RollCall.CreateRollCallExamples.R_ID


object CloseRollCallExamples {

  final val SENDER: PublicKey = PublicKey(Base64Data("gUSKTlXcSHfQmHbKYsa0obpotjoc-wwtkeKods9WBcY="))
  final val SIGNATURE: Signature = Signature(Base64Data("nyb5LwNBnw-kAMUI-p9zNmwDWXNBIXeSadGV-h7Kq2TIlezYTTt8S3nEQgEgSlvuvSR7UPy5byJFhiOKdws2Bg=="))

  final val LAO_ID: Hash = Hash(Base64Data.encode("laoId"))
  final val NOT_STALE_CLOSED_AT = Timestamp(1649089861L)
  final val CLOSES: Hash = OpenRollCallExamples.UPDATE_ID
  final val UPDATE_ID: Hash = Hash.fromStrings(EVENT_HASH_PREFIX, LAO_ID.toString, CLOSES.toString, NOT_STALE_CLOSED_AT.toString)
  final val ATTENDEES: List[PublicKey]  = List(SENDER)

  val invalidTimestamp: Timestamp = Timestamp(0)
  val invalidId: Hash = Hash(Base64Data.encode("wrong"))
  val invalidSender: PublicKey = PublicKey(Base64Data.encode("wrong"))

  val workingClosesRollCall: CloseRollCall = CloseRollCall(UPDATE_ID, CLOSES, NOT_STALE_CLOSED_AT, ATTENDEES)
  final val DATA_CLOSE_ROLL_CALL_WORKING: Base64Data = Base64Data.encode(workingClosesRollCall.toJson.toString)
  final val MESSAGE_CLOSE_ROLL_CALL_WORKING: Message = new Message(
    DATA_CLOSE_ROLL_CALL_WORKING,
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(workingClosesRollCall)
  )

  val wrongTimestampCloseRollCall: CloseRollCall = CloseRollCall(UPDATE_ID, CLOSES, invalidTimestamp, ATTENDEES)
  final val MESSAGE_CLOSE_ROLL_CALL_WRONG_TIMESTAMP: Message = new Message(
    Base64Data.encode(wrongTimestampCloseRollCall.toJson.toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongTimestampCloseRollCall)
  )


  val wrongIdCloseRollCall: CloseRollCall = CloseRollCall(invalidId, CLOSES, NOT_STALE_CLOSED_AT, ATTENDEES)
  final val MESSAGE_CLOSE_ROLL_CALL_WRONG_ID: Message = new Message(
    Base64Data.encode(wrongIdCloseRollCall.toJson.toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongIdCloseRollCall)
  )

  val wrongClosesCloseRollCall: CloseRollCall = CloseRollCall(UPDATE_ID, invalidId, NOT_STALE_CLOSED_AT, ATTENDEES)
  final val DATA_CLOSE_ROLL_CALL_WRONG_CLOSES: Base64Data = Base64Data.encode(wrongClosesCloseRollCall.toJson.toString)
  final val MESSAGE_CLOSE_ROLL_CALL_WRONG_CLOSES: Message = new Message(
    DATA_CLOSE_ROLL_CALL_WRONG_CLOSES,
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongClosesCloseRollCall)
  )


  val wrongAlreadyClosedCloseRollCall: CloseRollCall = CloseRollCall(CLOSES, CLOSES, NOT_STALE_CLOSED_AT, ATTENDEES)
  final val MESSAGE_CLOSE_ROLL_CALL_ALREADY_CLOSED: Message = new Message(
    Base64Data.encode(wrongAlreadyClosedCloseRollCall.toJson.toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongAlreadyClosedCloseRollCall)
  )

  val wrongAttendeesCloseRollCall: CloseRollCall = CloseRollCall(UPDATE_ID, CLOSES, NOT_STALE_CLOSED_AT, List(invalidSender))
  final val MESSAGE_CLOSE_ROLL_CALL_WRONG_ATTENDEES: Message = new Message(
    Base64Data.encode(wrongAttendeesCloseRollCall.toJson.toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongAttendeesCloseRollCall)
  )

  val wrongDuplicateAttendeesCloseRollCall: CloseRollCall = CloseRollCall(UPDATE_ID, CLOSES, NOT_STALE_CLOSED_AT, List(SENDER, SENDER))
  final val MESSAGE_CLOSE_ROLL_CALL_WRONG_DUPLICATE_ATTENDEES: Message = new Message(
    Base64Data.encode(wrongDuplicateAttendeesCloseRollCall.toJson.toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongDuplicateAttendeesCloseRollCall)
  )

  val wrongSenderCloseRollCall: CloseRollCall = CloseRollCall(UPDATE_ID, CLOSES, NOT_STALE_CLOSED_AT, ATTENDEES)
  final val MESSAGE_CLOSE_ROLL_CALL_WRONG_SENDER: Message = new Message(
    Base64Data.encode(wrongSenderCloseRollCall.toJson.toString),
    invalidSender,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongSenderCloseRollCall)
  )
}
