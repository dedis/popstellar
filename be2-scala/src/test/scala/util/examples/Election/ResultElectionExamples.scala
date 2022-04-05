package util.examples.Election

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.election.{ElectionBallotVotes, ElectionQuestionResult, ResultElection}
import ch.epfl.pop.model.objects._
import spray.json._


object ResultElectionExamples {

  final val SENDER: PublicKey = SetupElectionExamples.SENDER_SETUPELECTION
  final val VOTING_METHOD: String = SetupElectionExamples.VOTING_METHOD
  final val SIGNATURE: Signature = SetupElectionExamples.SIGNATURE

  final val ID_QUESTION: Hash = Hash(Base64Data.encode("question"))
  final val QUESTIONS: List[ElectionQuestionResult] = List(ElectionQuestionResult(ID_QUESTION, List(ElectionBallotVotes("yes", 1))))

  val invalidSender: PublicKey = PublicKey(Base64Data.encode("wrong"))

  val workingResultElection: ResultElection = ResultElection(QUESTIONS, List.empty)
  final val MESSAGE_RESULT_ELECTION_WORKING: Message = new Message(
    Base64Data.encode(workingResultElection.toJson.toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(workingResultElection)
  )

  final val MESSAGE_RESULT_ELECTION_WRONG_OWNER: Message = new Message(
    Base64Data.encode(workingResultElection.toJson.toString),
    invalidSender,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(workingResultElection)
  )
}
