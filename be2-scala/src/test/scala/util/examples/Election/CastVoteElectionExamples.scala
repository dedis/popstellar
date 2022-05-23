package util.examples.Election

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.election.{CastVoteElection, ElectionQuestion, SetupElection, VoteElection}
import ch.epfl.pop.model.objects._
import spray.json._
import util.examples.Election.SetupElectionExamples.ELECTION_ID


object CastVoteElectionExamples {

  final val SENDER: PublicKey = SetupElectionExamples.SENDER_SETUPELECTION
  final val VOTING_METHOD: String = SetupElectionExamples.VOTING_METHOD
  final val SIGNATURE: Signature = SetupElectionExamples.SIGNATURE

  final val ID: Hash = ELECTION_ID
  final val LAO_ID: Hash = Hash(Base64Data.encode("laoId"))
  final val NOT_STALE_CREATED_AT = Timestamp(1649089855L)
  final val QUESTION_ID = Hash.fromStrings("Question", ID.toString, "valid")
  final val VOTE = Some(List(1))
  final val VOTE_ID = Hash.fromStrings("Vote", ID.toString, QUESTION_ID.toString, VOTE.get.head.toString)
  final val VOTES: List[VoteElection] = List(VoteElection(VOTE_ID, QUESTION_ID, Some(List(1)), None))

  val invalidTimestamp: Timestamp = Timestamp(0)
  val invalidId: Hash = Hash(Base64Data.encode("wrong"))
  val invalidSender: PublicKey = PublicKey(Base64Data.encode("wrong"))
  val invalidVotes: List[VoteElection] = List(VoteElection(VOTE_ID, invalidId, Some(List(1)), None))
  val invalidBallot: List[VoteElection] = List(VoteElection(VOTE_ID, QUESTION_ID, Some(List(2)), None))
  val invalidVoteId:  List[VoteElection] = List(VoteElection(invalidId, QUESTION_ID, Some(List(1)), None))


  val workingCastVoteElection: CastVoteElection = CastVoteElection(LAO_ID, ID, NOT_STALE_CREATED_AT, VOTES)
  val DATA_CAST_VOTE_MESSAGE: Hash = Hash(Base64Data.encode(workingCastVoteElection.toJson.toString))
  final val MESSAGE_CAST_VOTE_ELECTION_WORKING: Message = new Message(
    DATA_CAST_VOTE_MESSAGE.base64Data,
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(workingCastVoteElection)
  )

  val wrongTimestampCastVoteElection: CastVoteElection = CastVoteElection(LAO_ID, ID, invalidTimestamp, VOTES)
  final val MESSAGE_CAST_VOTE_ELECTION_WRONG_TIMESTAMP: Message = new Message(
    Base64Data.encode(wrongTimestampCastVoteElection.toJson.toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongTimestampCastVoteElection)
  )

  val wrongIdCastVoteElection: CastVoteElection = CastVoteElection(LAO_ID, invalidId, NOT_STALE_CREATED_AT, VOTES)
  final val MESSAGE_CAST_VOTE_ELECTION_WRONG_ID: Message = new Message(
    Base64Data.encode(wrongIdCastVoteElection.toJson.toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongIdCastVoteElection)
  )

  val wrongLaoIdCastVoteElection: CastVoteElection = CastVoteElection(invalidId, ID, NOT_STALE_CREATED_AT, VOTES)
  final val MESSAGE_CAST_VOTE_ELECTION_WRONG_LAO_ID: Message = new Message(
    Base64Data.encode(wrongLaoIdCastVoteElection.toJson.toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(wrongIdCastVoteElection)
  )

  final val MESSAGE_CAST_VOTE_ELECTION_WRONG_OWNER: Message = new Message(
    Base64Data.encode(workingCastVoteElection.toJson.toString),
    invalidSender,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(workingCastVoteElection)
  )

  val invalidVoteQuestionIdCastVoteElection: CastVoteElection = CastVoteElection(LAO_ID, ID, NOT_STALE_CREATED_AT, invalidVotes)
  val DATA_CAST_VOTE_INVALID_VOTES: Hash = Hash(Base64Data.encode(invalidVoteQuestionIdCastVoteElection.toJson.toString))
  final val MESSAGE_CAST_VOTE_INVALID_VOTES: Message = new Message(
    DATA_CAST_VOTE_INVALID_VOTES.base64Data,
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(invalidVoteQuestionIdCastVoteElection)
  )

  val invalidBallotCastVoteElection: CastVoteElection = CastVoteElection(LAO_ID, ID, NOT_STALE_CREATED_AT, invalidBallot)
  val DATA_CAST_VOTE_INVALID_BALLOT: Hash = Hash(Base64Data.encode(invalidBallotCastVoteElection.toJson.toString))
  final val MESSAGE_CAST_VOTE_INVALID_BALLOT: Message = new Message(
    DATA_CAST_VOTE_INVALID_BALLOT.base64Data,
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(invalidBallotCastVoteElection)
  )
  
  val invalidVoteIdCastVoteElection: CastVoteElection = CastVoteElection(LAO_ID, ID, NOT_STALE_CREATED_AT, invalidVoteId)
  val DATA_CAST_VOTE_INVALID_VOTE_ID: Hash = Hash(Base64Data.encode(invalidVoteIdCastVoteElection.toJson.toString))
  final val MESSAGE_CAST_VOTE_INVALID_VOTE_ID: Message = new Message(
    DATA_CAST_VOTE_INVALID_VOTE_ID.base64Data,
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(invalidVoteIdCastVoteElection)
  )
}
