package util.examples.Election

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.election.{ElectionQuestion, SetupElection}
import ch.epfl.pop.model.objects._
import spray.json._


object SetupElectionExamples {

  final val SENDER_SETUPELECTION: PublicKey = PublicKey(Base64Data("gUSKTlXcSHfQmHbKYsa0obpotjoc-wwtkeKods9WBcY="))
  final val VOTING_METHOD: String = "Plurality"
  final val SIGNATURE: Signature = Signature(Base64Data("nyb5LwNBnw-kAMUI-p9zNmwDWXNBIXeSadGV-h7Kq2TIlezYTTt8S3nEQgEgSlvuvSR7UPy5byJFhiOKdws2Bg=="))

  final val ID: Hash = Hash(Base64Data("fSk3oxJfmhUlRzhpP2rLypTtxboGMhfFaaYKDeZn1SY="))
  final val LAO_ID: Hash = Hash(Base64Data.encode("laoId"))
  final val ELECTION_NAME: String = "valid"
  final val ELECTION_VERSION: String = ""
  final val NOT_STALE_CREATED_AT = Timestamp(1649089855L)
  final val NOT_STALE_START_TIME = Timestamp(1649089860L)
  final val NOT_STALE_END_TIME = Timestamp(1649093440L)
  final val QUESTIONS = List(ElectionQuestion(Hash(Base64Data("1I1mAuxuZsAFX2mYf4ZsU2xeAw6oadTIkBlMMZvivpo=")), "valid", VOTING_METHOD, List("yes","no"), false))
  final val ELECTION_ID: Hash = Hash.fromStrings("Election", LAO_ID.toString, NOT_STALE_CREATED_AT.toString, ELECTION_NAME)

  val invalidTimestamp: Timestamp = Timestamp(0)
  val invalidId: Hash = Hash(Base64Data.encode("wrong"))
  val invalidSender: PublicKey = PublicKey(Base64Data.encode("wrong"))

  val workingSetupElection: SetupElection = SetupElection(ELECTION_ID, LAO_ID, ELECTION_NAME, ELECTION_VERSION, NOT_STALE_CREATED_AT, NOT_STALE_START_TIME, NOT_STALE_END_TIME, QUESTIONS)
  final val DATA_SET_UP_MESSAGE: Hash = Hash(Base64Data.encode(workingSetupElection.toJson.toString))
  final val MESSAGE_SETUPELECTION_WORKING: Message = new Message(
    DATA_SET_UP_MESSAGE.base64Data,
    SENDER_SETUPELECTION,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(workingSetupElection)
  )

  val wrongTimestampSetupElection: SetupElection = SetupElection(ELECTION_ID, LAO_ID, ELECTION_NAME, ELECTION_VERSION, invalidTimestamp, NOT_STALE_START_TIME, NOT_STALE_END_TIME, QUESTIONS)
  final val MESSAGE_SETUPELECTION_WRONG_TIMESTAMP: Message = new Message(
    Base64Data.encode(wrongTimestampSetupElection.toJson.toString),
    SENDER_SETUPELECTION,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongTimestampSetupElection)
  )

  val wrongTimestampOrderSetupElection: SetupElection = SetupElection(ELECTION_ID, LAO_ID, ELECTION_NAME, ELECTION_VERSION, NOT_STALE_CREATED_AT, NOT_STALE_END_TIME, NOT_STALE_START_TIME, QUESTIONS)
  final val MESSAGE_SETUPELECTION_WRONG_ORDER: Message = new Message(
    Base64Data.encode(wrongTimestampOrderSetupElection.toJson.toString),
    SENDER_SETUPELECTION,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongTimestampOrderSetupElection)
  )

  val wrongTimestampOrderSetupElection2: SetupElection = SetupElection(ELECTION_ID, LAO_ID, ELECTION_NAME, ELECTION_VERSION, NOT_STALE_START_TIME, NOT_STALE_CREATED_AT, NOT_STALE_END_TIME, QUESTIONS)
  final val MESSAGE_SETUPELECTION_WRONG_ORDER2: Message = new Message(
    Base64Data.encode(wrongTimestampOrderSetupElection2.toJson.toString),
    SENDER_SETUPELECTION,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongTimestampOrderSetupElection2)
  )

  val wrongIdSetupElection: SetupElection = SetupElection(invalidId, LAO_ID, ELECTION_NAME, ELECTION_VERSION, NOT_STALE_CREATED_AT, NOT_STALE_START_TIME, NOT_STALE_END_TIME, QUESTIONS)
  final val MESSAGE_SETUPELECTION_WRONG_ID: Message = new Message(
    Base64Data.encode(wrongIdSetupElection.toJson.toString),
    SENDER_SETUPELECTION,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongIdSetupElection)
  )

  final val MESSAGE_SETUPELECTION_WRONG_OWNER: Message = new Message(
    Base64Data.encode(workingSetupElection.toJson.toString),
    invalidSender,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(workingSetupElection)
  )
}
