package util.examples.Election

import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.election.{ElectionBallotVotes, ElectionQuestionResult, ResultElection}
import ch.epfl.pop.model.objects._
import spray.json._
import ch.epfl.pop.json.MessageDataProtocol._

object ResultElectionExamples {

  final val SENDER_RESULT_ELECTION: PublicKey = PublicKey(Base64Data("gUSKTlXcSHfQmHbKYsa0obpotjoc-wwtkeKods9WBcY="))

  final val SIGNATURE: Signature = SetupElectionExamples.SIGNATURE

  final val ELECTION_ID: Hash = SetupElectionExamples.ELECTION_ID

  val workingElectionQuestionResult: ElectionQuestionResult = ElectionQuestionResult(Hash.fromStrings("Question", ELECTION_ID.toString, "valid"), List(ElectionBallotVotes("yes", 2), ElectionBallotVotes("no", 1)))

  val wrongBallotOptionsQuestionResult: ElectionQuestionResult = ElectionQuestionResult(Hash.fromStrings("Question", ELECTION_ID.toString, "valid"), List(ElectionBallotVotes("maybe", 2), ElectionBallotVotes("sometimes", 1)))

  val wrongElectionQuestionResult: ElectionQuestionResult = ElectionQuestionResult(Hash.fromStrings("Question", ELECTION_ID.toString, "valid"), List(ElectionBallotVotes("yes", 2), ElectionBallotVotes("no", -1)))

  val tooMuchVotesElectionQuestionResult: ElectionQuestionResult = ElectionQuestionResult(Hash.fromStrings("Question", ELECTION_ID.toString, "valid"), List(ElectionBallotVotes("yes", 2), ElectionBallotVotes("no", 2)))

  val wrongIdElectionQuestionResult: ElectionQuestionResult = ElectionQuestionResult(Hash.fromStrings("WrongHash", ELECTION_ID.toString, "valid"), List(ElectionBallotVotes("yes", 2), ElectionBallotVotes("no", 1)))

  val workingResultElection: ResultElection = ResultElection(List(workingElectionQuestionResult), List(SIGNATURE))

  val wrongBallotOptionsElection: ResultElection = ResultElection(List(wrongBallotOptionsQuestionResult), List(SIGNATURE))

  val wrongResultElection: ResultElection = ResultElection(List(wrongElectionQuestionResult), List(SIGNATURE))

  val tooMuchVotesElection: ResultElection = ResultElection(List(tooMuchVotesElectionQuestionResult), List(SIGNATURE))

  val wrongIdElection: ResultElection = ResultElection(List(wrongIdElectionQuestionResult), List(SIGNATURE))

  final val DATA_RESULT_ELECTION_WORKING: Hash = Hash(Base64Data.encode(workingResultElection.toJson.toString))

  final val DATA_RESULT_ELECTION_WRONG_BALLOT_OPTIONS: Hash = Hash(Base64Data.encode(wrongBallotOptionsElection.toJson.toString))

  final val DATA_RESULT_ELECTION_WRONG: Hash = Hash(Base64Data.encode(wrongResultElection.toJson.toString))

  final val DATA_RESULT_ELECTION_TOO_MUCH_VOTES: Hash = Hash(Base64Data.encode(tooMuchVotesElectionQuestionResult.toJson.toString))

  final val DATA_RESULT_ELECTION_WRONG_ID: Hash = Hash(Base64Data.encode(wrongIdElection.toJson.toString))

  final val MESSAGE_RESULT_ELECTION_WORKING: Message = new Message(
    DATA_RESULT_ELECTION_WORKING.base64Data,
    SENDER_RESULT_ELECTION,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(workingResultElection)
  )

  final val MESSAGE_RESULT_ELECTION_WRONG_BALLOT_OPTIONS: Message = new Message(
    DATA_RESULT_ELECTION_WRONG_BALLOT_OPTIONS.base64Data,
    SENDER_RESULT_ELECTION,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongBallotOptionsElection)
  )

  final val MESSAGE_RESULT_ELECTION_WRONG: Message = new Message(
    DATA_RESULT_ELECTION_WRONG.base64Data,
    SENDER_RESULT_ELECTION,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongResultElection)
  )

  final val MESSAGE_RESULT_ELECTION_TOO_MUCH_VOTES: Message = new Message(
    DATA_RESULT_ELECTION_TOO_MUCH_VOTES.base64Data,
    SENDER_RESULT_ELECTION,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(tooMuchVotesElection)
  )

  final val MESSAGE_RESULT_ELECTION_WRONG_ID: Message = new Message(
    DATA_RESULT_ELECTION_WRONG_ID.base64Data,
    SENDER_RESULT_ELECTION,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongIdElection)
  )

}
