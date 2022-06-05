package util.examples.RollCall

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.rollCall.CreateRollCall
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.graph.validators.RollCallValidator.EVENT_HASH_PREFIX
import spray.json._


object CreateRollCallExamples {

  final val SENDER: PublicKey = PublicKey(Base64Data("gUSKTlXcSHfQmHbKYsa0obpotjoc-wwtkeKods9WBcY="))
  final val SIGNATURE: Signature = Signature(Base64Data("nyb5LwNBnw-kAMUI-p9zNmwDWXNBIXeSadGV-h7Kq2TIlezYTTt8S3nEQgEgSlvuvSR7UPy5byJFhiOKdws2Bg=="))

  final val LAO_ID: Hash = Hash(Base64Data.encode("laoId"))
  final val NAME: String = "roll call"
  final val NOT_STALE_CREATION = Timestamp(1649089860L)
  final val NOT_STALE_PROPOSED_START = Timestamp(1649089861L)
  final val NOT_STALE_PROPOSED_END = Timestamp(1649089869L)
  final val R_ID: Hash = Hash.fromStrings(EVENT_HASH_PREFIX, LAO_ID.toString, NOT_STALE_CREATION.toString, NAME)

  val invalidTimestamp: Timestamp = Timestamp(0)
  val invalidId: Hash = Hash(Base64Data.encode("wrong"))
  val invalidSender: PublicKey = PublicKey(Base64Data.encode("wrong"))

  val workingCreatRollCall: CreateRollCall = CreateRollCall(R_ID, NAME, NOT_STALE_CREATION, NOT_STALE_PROPOSED_START, NOT_STALE_PROPOSED_END, "", None)
  final val MESSAGE_CREATE_ROLL_CALL_WORKING: Message = new Message(
    Base64Data.encode(workingCreatRollCall.toJson.toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(workingCreatRollCall)
  )

  val wrongTimestampCreateRollCall: CreateRollCall = CreateRollCall(R_ID, NAME, invalidTimestamp, NOT_STALE_PROPOSED_START, NOT_STALE_PROPOSED_END, "", None)
  final val MESSAGE_CREATE_ROLL_CALL_WRONG_TIMESTAMP: Message = new Message(
    Base64Data.encode(wrongTimestampCreateRollCall.toJson.toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongTimestampCreateRollCall)
  )

  val wrongTimestampOrderCreateRollCall: CreateRollCall = CreateRollCall(R_ID, NAME, NOT_STALE_CREATION, NOT_STALE_PROPOSED_START, invalidTimestamp, "", None)
  final val MESSAGE_CREATE_ROLL_CALL_WRONG_TIMESTAMP_ORDER: Message = new Message(
    Base64Data.encode(wrongTimestampOrderCreateRollCall.toJson.toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongTimestampOrderCreateRollCall)
  )

  val wrongIdRollCall: CreateRollCall = CreateRollCall(invalidId, NAME, NOT_STALE_CREATION, NOT_STALE_PROPOSED_START, NOT_STALE_PROPOSED_END, "", None)
  final val MESSAGE_CREATE_ROLL_CALL_WRONG_ID: Message = new Message(
    Base64Data.encode(wrongIdRollCall.toJson.toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongIdRollCall)
  )

  val wrongSenderRollCall: CreateRollCall = CreateRollCall(R_ID, NAME, NOT_STALE_CREATION, NOT_STALE_PROPOSED_START, NOT_STALE_PROPOSED_END, "", None)
  final val MESSAGE_CREATE_ROLL_CALL_WRONG_SENDER: Message = new Message(
    Base64Data.encode(wrongSenderRollCall.toJson.toString),
    invalidSender,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongSenderRollCall)
  )
}
