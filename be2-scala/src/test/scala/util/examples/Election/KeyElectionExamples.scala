package util.examples.Election

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.election.{KeyElection, OpenElection}
import ch.epfl.pop.model.objects._
import spray.json._
import util.examples.Election.OpenElectionExamples.workingOpenElection


object KeyElectionExamples {

  final val SENDER_ELECTION: PublicKey = SetupElectionExamples.SENDER_SETUPELECTION
  final val VOTING_METHOD: String = SetupElectionExamples.VOTING_METHOD
  final val SIGNATURE: Signature = SetupElectionExamples.SIGNATURE

  final val ELECTION_KEY: PublicKey = PublicKey(Base64Data.encode("key"))
  final val ELECTION_ID: Hash = Hash(Base64Data.encode("election"))

  val invalidId: Hash = Hash(Base64Data.encode("wrong"))
  val invalidSender: PublicKey = PublicKey(Base64Data.encode("wrong"))

  val workingKeyElection: KeyElection = KeyElection(ELECTION_ID, ELECTION_KEY)
  final val MESSAGE_KEY_ELECTION_WORKING: Message = new Message(
    Base64Data.encode(workingOpenElection.toJson.toString),
    SENDER_ELECTION,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(workingKeyElection)
  )

  val wrongElectionIdKeyElection: KeyElection = KeyElection(invalidId, ELECTION_KEY)
  final val MESSAGE_KEY_ELECTION_WRONG_ELECTION_ID: Message = new Message(
    Base64Data.encode(wrongElectionIdKeyElection.toJson.toString),
    SENDER_ELECTION,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongElectionIdKeyElection)
  )


  final val MESSAGE_KEY_ELECTION_WRONG_OWNER: Message = new Message(
    Base64Data.encode(workingKeyElection.toJson.toString),
    invalidSender,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(workingKeyElection)
  )
}
