package util.examples.Election

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.election.{CastVoteElection, ElectionQuestion, SetupElection, VoteElection}
import ch.epfl.pop.model.objects._
import spray.json._
import util.examples.Election.CastVoteElectionExamples.{invalidBallot, invalidVotes}


object CastVoteElectionExamples {

  final val SENDER: PublicKey = SetupElectionExamples.SENDER_SETUPELECTION
  final val VOTING_METHOD: String = SetupElectionExamples.VOTING_METHOD
  final val SIGNATURE: Signature = SetupElectionExamples.SIGNATURE

  final val ID: Hash = Hash(Base64Data.encode("election"))
  final val LAO_ID: Hash = Hash(Base64Data.encode("laoId"))
  final val NOT_STALE_CREATED_AT = Timestamp(1649089855L)
  final val VOTES: List[VoteElection] = List(VoteElection(Hash(Base64Data("KNRSAzia1ngjKZPwBpHQIYssS33VBl3eP5LWDUikAh4=")), Hash(Base64Data("xPwqh_6mHXRFYseArRJmrZjR8vc_jKaSQL8ZtToEozo=")), Some(1), None))

  val invalidTimestamp: Timestamp = Timestamp(0)
  val invalidId: Hash = Hash(Base64Data.encode("wrong"))
  val invalidSender: PublicKey = PublicKey(Base64Data.encode("wrong"))
  val invalidVotes: List[VoteElection] = List(VoteElection(Hash(Base64Data("KNRSAzia1ngjKZPwBpHQIYssS33VBl3eP5LWDUikAh4=")), invalidId, Some(1), None))
  val invalidBallot: List[VoteElection] = List(VoteElection(Hash(Base64Data("KNRSAzia1ngjKZPwBpHQIYssS33VBl3eP5LWDUikAh4=")), Hash(Base64Data("1I1mAuxuZsAFX2mYf4ZsU2xeAw6oadTIkBlMMZvivpo=")), Some(2), None))


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

  val invalidVoteCastVoteElection: CastVoteElection = CastVoteElection(LAO_ID, ID, NOT_STALE_CREATED_AT, invalidVotes)
  val DATA_CAST_VOTE_INVALID_VOTES: Hash = Hash(Base64Data.encode(invalidVoteCastVoteElection.toJson.toString))
  final val MESSAGE_CAST_VOTE_INVALID_VOTES: Message = new Message(
    DATA_CAST_VOTE_INVALID_VOTES.base64Data,
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(invalidVoteCastVoteElection)
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
}
