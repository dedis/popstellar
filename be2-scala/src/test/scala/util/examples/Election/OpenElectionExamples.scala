package util.examples.Election

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.election.OpenElection
import ch.epfl.pop.model.objects._
import spray.json._

object OpenElectionExamples {

  final val SENDER_ELECTION: PublicKey = SetupElectionExamples.SENDER_SETUPELECTION
  final val VOTING_METHOD: String = SetupElectionExamples.VOTING_METHOD
  final val SIGNATURE: Signature = SetupElectionExamples.SIGNATURE

  final val LAO_ID: Hash = Hash(Base64Data.encode("laoId"))
  final val NOT_STALE_OPENED_AT: Timestamp = Timestamp(1649089860L)
  final val ELECTION_ID: Hash = Hash(Base64Data.encode("election"))

  val invalidTimestamp: Timestamp = Timestamp(0)
  val invalidId: Hash = Hash(Base64Data.encode("wrong"))
  val invalidSender: PublicKey = PublicKey(Base64Data.encode("wrong"))

  val workingOpenElection: OpenElection = OpenElection(LAO_ID, ELECTION_ID, NOT_STALE_OPENED_AT)
  final val DATA_OPEN_MESSAGE: Hash = Hash(Base64Data.encode(workingOpenElection.toJson.toString))
  final val MESSAGE_OPEN_ELECTION_WORKING: Message = new Message(
    DATA_OPEN_MESSAGE.base64Data,
    SENDER_ELECTION,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(workingOpenElection)
  )

  val wrongTimestampOpenElection: OpenElection = OpenElection(LAO_ID, ELECTION_ID, invalidTimestamp)
  final val MESSAGE_OPEN_ELECTION_WRONG_TIMESTAMP: Message = new Message(
    Base64Data.encode(wrongTimestampOpenElection.toJson.toString),
    SENDER_ELECTION,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongTimestampOpenElection)
  )

  val wrongIdOpenElection: OpenElection = OpenElection(LAO_ID, invalidId, NOT_STALE_OPENED_AT)
  final val MESSAGE_OPEN_ELECTION_WRONG_ID: Message = new Message(
    Base64Data.encode(wrongIdOpenElection.toJson.toString),
    SENDER_ELECTION,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongIdOpenElection)
  )

  val wrongLaoIdOpenElection: OpenElection = OpenElection(invalidId, ELECTION_ID, NOT_STALE_OPENED_AT)
  final val MESSAGE_OPEN_ELECTION_WRONG_LAO_ID: Message = new Message(
    Base64Data.encode(wrongLaoIdOpenElection.toJson.toString),
    SENDER_ELECTION,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongLaoIdOpenElection)
  )

  final val MESSAGE_OPEN_ELECTION_WRONG_OWNER: Message = new Message(
    Base64Data.encode(workingOpenElection.toJson.toString),
    invalidSender,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(workingOpenElection)
  )
}
