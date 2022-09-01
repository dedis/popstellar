package util.examples.RollCall

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.rollCall._
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.graph.validators.RollCallValidator.EVENT_HASH_PREFIX
import spray.json._
import util.examples.RollCall.CreateRollCallExamples.R_ID

object OpenRollCallExamples {

  final val SENDER: PublicKey = PublicKey(Base64Data("gUSKTlXcSHfQmHbKYsa0obpotjoc-wwtkeKods9WBcY="))
  final val SIGNATURE: Signature = Signature(Base64Data("nyb5LwNBnw-kAMUI-p9zNmwDWXNBIXeSadGV-h7Kq2TIlezYTTt8S3nEQgEgSlvuvSR7UPy5byJFhiOKdws2Bg=="))

  final val LAO_ID: Hash = Hash(Base64Data.encode("laoId"))
  final val NOT_STALE_OPENED_AT: Timestamp = Timestamp(1649089861L)
  final val OPENS: Hash = R_ID
  final val UPDATE_ID: Hash = Hash.fromStrings(EVENT_HASH_PREFIX, LAO_ID.toString, OPENS.toString, NOT_STALE_OPENED_AT.toString)
  final val UPDATE_ID2: Hash = Hash.fromStrings(EVENT_HASH_PREFIX, LAO_ID.toString, CloseRollCallExamples.UPDATE_ID.toString, NOT_STALE_OPENED_AT.toString)

  val invalidTimestamp: Timestamp = Timestamp(0)
  val invalidId: Hash = Hash(Base64Data.encode("wrong"))
  val invalidSender: PublicKey = PublicKey(Base64Data.encode("wrong"))

  val workingOpenRollCall: OpenRollCall = OpenRollCall(UPDATE_ID, OPENS, NOT_STALE_OPENED_AT)
  final val DATA_OPEN_ROLL_CALL: Base64Data = Base64Data.encode(workingOpenRollCall.toJson.toString)
  final val MESSAGE_OPEN_ROLL_CALL_WORKING: Message = new Message(
    DATA_OPEN_ROLL_CALL,
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(workingOpenRollCall)
  )

  val wrongTimestampOpenRollCall: OpenRollCall = OpenRollCall(UPDATE_ID, OPENS, invalidTimestamp)
  final val MESSAGE_OPEN_ROLL_CALL_WRONG_TIMESTAMP: Message = new Message(
    Base64Data.encode(wrongTimestampOpenRollCall.toJson.toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongTimestampOpenRollCall)
  )

  val wrongIdOpenRollCall: OpenRollCall = OpenRollCall(invalidId, OPENS, NOT_STALE_OPENED_AT)
  final val MESSAGE_OPEN_ROLL_CALL_WRONG_ID: Message = new Message(
    Base64Data.encode(wrongIdOpenRollCall.toJson.toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongIdOpenRollCall)
  )

  val wrongOpensOpenRollCall: OpenRollCall = OpenRollCall(UPDATE_ID, invalidId, NOT_STALE_OPENED_AT)
  final val DATA_OPEN_ROLL_CALL_WRONG_OPENS: Base64Data = Base64Data.encode(wrongOpensOpenRollCall.toJson.toString)
  final val MESSAGE_OPEN_ROLL_CALL_WRONG_OPENS: Message = new Message(
    DATA_OPEN_ROLL_CALL_WRONG_OPENS,
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongOpensOpenRollCall)
  )

  val validOpensOpenRollCall: OpenRollCall = OpenRollCall(UPDATE_ID2, CloseRollCallExamples.UPDATE_ID, NOT_STALE_OPENED_AT)
  final val DATA_OPEN_ROLL_CALL_VALID_OPENS: Base64Data = Base64Data.encode(validOpensOpenRollCall.toJson.toString)
  final val MESSAGE_OPEN_ROLL_CALL_VALID_OPENS: Message = new Message(
    DATA_OPEN_ROLL_CALL_VALID_OPENS,
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(validOpensOpenRollCall)
  )

  val wrongSenderOpenRollCall: OpenRollCall = OpenRollCall(UPDATE_ID, OPENS, NOT_STALE_OPENED_AT)
  final val MESSAGE_OPEN_ROLL_CALL_WRONG_SENDER: Message = new Message(
    Base64Data.encode(wrongSenderOpenRollCall.toJson.toString),
    invalidSender,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongSenderOpenRollCall)
  )
}
