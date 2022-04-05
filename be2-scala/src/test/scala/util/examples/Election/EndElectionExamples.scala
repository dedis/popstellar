package util.examples.Election

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.election.EndElection
import ch.epfl.pop.model.objects._
import spray.json._


object EndElectionExamples {

  final val SENDER: PublicKey = SetupElectionExamples.SENDER_SETUPELECTION
  final val VOTING_METHOD: String = SetupElectionExamples.VOTING_METHOD
  final val SIGNATURE: Signature = SetupElectionExamples.SIGNATURE

  final val ID: Hash = Hash(Base64Data.encode("election"))
  final val LOA_ID: Hash = Hash(Base64Data.encode("laoId"))
  final val NOT_STALE_CREATED_AT = Timestamp(1649089855L)
  final val REGISTERED_VOTES: Hash = Hash(Base64Data("47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU"))

  val invalidTimestamp: Timestamp = Timestamp(0)
  val invalidId: Hash = Hash(Base64Data.encode("wrong"))
  val invalidSender: PublicKey = PublicKey(Base64Data.encode("wrong"))

  val workingEndElection: EndElection = EndElection(LOA_ID, ID, NOT_STALE_CREATED_AT, REGISTERED_VOTES)
  final val MESSAGE_END_ELECTION_WORKING: Message = new Message(
    Base64Data.encode(workingEndElection.toJson.toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(workingEndElection)
  )

  val wrongTimestampEndElection: EndElection = EndElection(LOA_ID, ID, invalidTimestamp, REGISTERED_VOTES)
  final val MESSAGE_END_ELECTION_WRONG_TIMESTAMP: Message = new Message(
    Base64Data.encode(wrongTimestampEndElection.toJson.toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongTimestampEndElection)
  )

  val wrongIdEndElection: EndElection = EndElection(LOA_ID, invalidId, NOT_STALE_CREATED_AT, REGISTERED_VOTES)
  final val MESSAGE_END_ELECTION_WRONG_ID: Message = new Message(
    Base64Data.encode(wrongIdEndElection.toJson.toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongIdEndElection)
  )

  val wrongLaoIdEndElection: EndElection = EndElection(invalidId, ID, NOT_STALE_CREATED_AT, REGISTERED_VOTES)
  final val MESSAGE_END_ELECTION_WRONG_LAO_ID: Message = new Message(
    Base64Data.encode(wrongLaoIdEndElection.toJson.toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongIdEndElection)
  )

  final val MESSAGE_END_ELECTION_WRONG_OWNER: Message = new Message(
    Base64Data.encode(workingEndElection.toJson.toString),
    invalidSender,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(workingEndElection)
  )
}
