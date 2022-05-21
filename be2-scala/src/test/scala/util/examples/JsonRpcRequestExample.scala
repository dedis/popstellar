package util.examples

import ch.epfl.pop.model.network.method.{Params, ParamsWithMessage}
import ch.epfl.pop.model.network.{JsonRpcRequest, MethodType}
import ch.epfl.pop.model.objects.{Base64Data, Channel}
import util.examples.Election.CastVoteElectionExamples._
import util.examples.Election.OpenElectionExamples._
import util.examples.Election.SetupElectionExamples._
import util.examples.Election.EndElectionExamples._
import util.examples.Lao.GreetLaoExamples.{MESSAGE_GREET_LAO, MESSAGE_GREET_LAO_WRONG_ADDRESS, MESSAGE_GREET_LAO_WRONG_FRONTEND, MESSAGE_GREET_LAO_WRONG_LAO, MESSAGE_GREET_LAO_WRONG_OWNER}
import util.examples.MessageExample._
import util.examples.socialMedia.AddChirpExamples._
import util.examples.socialMedia.AddReactionExamples._
import util.examples.socialMedia.DeleteChirpExamples._
import util.examples.socialMedia.DeleteReactionExamples._

object JsonRpcRequestExample {

  private final val rpc: String = "rpc"
  private final val id: Option[Int] = Some(0)
  private final val methodType: MethodType.MethodType = MethodType.PUBLISH
  private final val channel: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + "channel")
  private final val paramsWithoutMessage: Params = new Params(channel)
  private final val paramsWithMessage: ParamsWithMessage = new ParamsWithMessage(channel, MESSAGE_WORKING_WS_PAIR)
  private final val paramsWithFaultyIdMessage: ParamsWithMessage = new ParamsWithMessage(channel, MESSAGE_FAULTY_ID)
  private final val paramsWithFaultyWSMessage: ParamsWithMessage = new ParamsWithMessage(channel, MESSAGE_FAULTY_WS_PAIR)
  private final val paramsWithFaultySignatureMessage: ParamsWithMessage = new ParamsWithMessage(channel, MESSAGE_FAULTY_SIGNATURE)

  final val VALID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithMessage, id)
  final val INVALID_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithFaultyIdMessage, id)
  final val INVALID_WS_PAIR_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithFaultyWSMessage, id)
  final val INVALID_SIGNATURE_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithFaultySignatureMessage, id)
  final val RPC_NO_PARAMS: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithoutMessage, id)

  // for CreateLao testing
  private final val paramsWithCreateLao: ParamsWithMessage = new ParamsWithMessage(Channel.ROOT_CHANNEL, MESSAGE_CREATELAO_WORKING)
  private final val paramsWithCreateLaoWrongTimestamp: ParamsWithMessage = new ParamsWithMessage(Channel.ROOT_CHANNEL, MESSAGE_CREATELAO_WRONG_TIMESTAMP)
  private final val paramsWithCreateLaoWrongChannel: ParamsWithMessage = new ParamsWithMessage(channel, MESSAGE_CREATELAO_WORKING)
  private final val paramsWithCreateLaoWrongWitnesses: ParamsWithMessage = new ParamsWithMessage(Channel.ROOT_CHANNEL, MESSAGE_CREATELAO_WRONG_WITNESSES)
  private final val paramsWithCreateLaoWrongId: ParamsWithMessage = new ParamsWithMessage(Channel.ROOT_CHANNEL, MESSAGE_CREATELAO_WRONG_ID)
  private final val paramsWithCreateLaoWrongSender: ParamsWithMessage = new ParamsWithMessage(Channel.ROOT_CHANNEL, MESSAGE_CREATELAO_WRONG_SENDER)
  private final val paramsWithCreateLaoEmptyName: ParamsWithMessage = new ParamsWithMessage(Channel.ROOT_CHANNEL, MESSAGE_CREATELAO_EMPTY_NAME)
  final val CREATE_LAO_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateLao, id)
  final val CREATE_LAO_WRONG_CHANNEL_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateLaoWrongChannel, id)
  final val CREATE_LAO_WRONG_TIMESTAMP_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateLaoWrongTimestamp, id)
  final val CREATE_LAO_WRONG_WITNESSES_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateLaoWrongWitnesses, id)
  final val CREATE_LAO_WRONG_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateLaoWrongId, id)
  final val CREATE_LAO_WRONG_SENDER_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateLaoWrongSender, id)
  final val CREATE_LAO_EMPTY_NAME_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateLaoEmptyName, id)

  //for GreetLao testing
  private final val laoChannel: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("laoId"))
  private final val paramsWithGreetLao: ParamsWithMessage = new ParamsWithMessage(laoChannel, MESSAGE_GREET_LAO)
  private final val paramsWithGreetLaoWrongFrontend: ParamsWithMessage = new ParamsWithMessage(laoChannel, MESSAGE_GREET_LAO_WRONG_FRONTEND)
  private final val paramsWithGreetLaoWrongAddress: ParamsWithMessage = new ParamsWithMessage(laoChannel, MESSAGE_GREET_LAO_WRONG_ADDRESS)
  private final val paramsWithGreetLaoWrongLao: ParamsWithMessage = new ParamsWithMessage(laoChannel, MESSAGE_GREET_LAO_WRONG_LAO)
  private final val paramsWithGreetLaoWrongSender: ParamsWithMessage = new ParamsWithMessage(laoChannel, MESSAGE_GREET_LAO_WRONG_OWNER)
  private final val paramsWithGreetLaoWrongChannel: ParamsWithMessage = new ParamsWithMessage(channel, MESSAGE_GREET_LAO)
  final val GREET_LAO_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithGreetLao, id)
  final val GREET_LAO_WRONG_FRONTEND_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithGreetLaoWrongFrontend, id)
  final val GREET_LAO_WRONG_ADDRESS_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithGreetLaoWrongAddress, id)
  final val GREET_LAO_WRONG_LAO_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithGreetLaoWrongLao, id)
  final val GREET_LAO_WRONG_SENDER_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithGreetLaoWrongSender, id)
  final val GREET_LAO_WRONG_CHANNEL_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithGreetLaoWrongChannel, id)


  // for AddChirp testing
  private final val rightSocialChannel: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("laoId") + Channel.SOCIAL_CHANNEL_PREFIX + SENDER_ADDCHIRP.base64Data)
  private final val wrongSocialChannel: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("laoId") + Channel.SOCIAL_CHANNEL_PREFIX + Base64Data.encode("channel"))
  private final val paramsWithAddChirp: ParamsWithMessage = new ParamsWithMessage(rightSocialChannel, MESSAGE_ADDCHIRP_WORKING)
  private final val paramsWithAddChirpWrongTimestamp: ParamsWithMessage = new ParamsWithMessage(rightSocialChannel, MESSAGE_ADDCHIRP_WRONG_TIMESTAMP)
  private final val paramsWithAddChirpWrongText: ParamsWithMessage = new ParamsWithMessage(rightSocialChannel, MESSAGE_ADDCHIRP_WRONG_TEXT)
  private final val paramsWithAddChirpWrongChannel: ParamsWithMessage = new ParamsWithMessage(wrongSocialChannel, MESSAGE_ADDCHIRP_WORKING)
  final val ADD_CHIRP_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithAddChirp, id)
  final val ADD_CHIRP_WRONG_CHANNEL_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithAddChirpWrongChannel, id)
  final val ADD_CHIRP_WRONG_TIMESTAMP_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithAddChirpWrongTimestamp, id)
  final val ADD_CHIRP_WRONG_TEXT_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithAddChirpWrongText, id)

  // for DeleteChirp testing (as the sender is the same in both MessageExamples, we can reuse the social channel from AddChirp test cases)
  private final val paramsWithDeleteChirp: ParamsWithMessage = new ParamsWithMessage(rightSocialChannel, MESSAGE_DELETECHIRP_WORKING)
  private final val paramsWithDeleteChirpWrongTimestamp: ParamsWithMessage = new ParamsWithMessage(rightSocialChannel, MESSAGE_DELETECHIRP_WRONG_TIMESTAMP)
  private final val paramsWithDeleteChirpWrongChannel: ParamsWithMessage = new ParamsWithMessage(wrongSocialChannel, MESSAGE_DELETECHIRP_WORKING)
  final val DELETE_CHIRP_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithDeleteChirp, id)
  final val DELETE_CHIRP_WRONG_CHANNEL_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithDeleteChirpWrongChannel, id)
  final val DELETE_CHIRP_WRONG_TIMESTAMP_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithDeleteChirpWrongTimestamp, id)

  // for AddReaction testing (the channel does not matter for them)
  private final val paramsWithAddReaction: ParamsWithMessage = new ParamsWithMessage(channel, MESSAGE_ADDREACTION_WORKING)
  private final val paramsWithAddReactionWrongTimestamp: ParamsWithMessage = new ParamsWithMessage(channel, MESSAGE_ADDREACTION_WRONG_TIMESTAMP)
  final val ADD_REACTION_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithAddReaction, id)
  final val ADD_REACTION_WRONG_TIMESTAMP_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithAddReactionWrongTimestamp, id)

  // for DeleteReaction testing (the channel does not matter for them)
  private final val paramsWithDeleteReaction: ParamsWithMessage = new ParamsWithMessage(channel, MESSAGE_DELETEREACTION_WORKING)
  private final val paramsWithDeleteReactionWrongTimestamp: ParamsWithMessage = new ParamsWithMessage(channel, MESSAGE_DELETEREACTION_WRONG_TIMESTAMP)
  final val DELETE_REACTION_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithDeleteReaction, id)
  final val DELETE_REACTION_WRONG_TIMESTAMP_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithDeleteReactionWrongTimestamp, id)

  // for ElectionSetup testing
  private final val rightElectionChannel: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("laoId"))
  private final val paramsWithSetupElection: ParamsWithMessage = new ParamsWithMessage(rightElectionChannel, MESSAGE_SETUPELECTION_WORKING)
  private final val paramsWithSetupElectionWrongTimestamp: ParamsWithMessage = new ParamsWithMessage(rightElectionChannel, MESSAGE_SETUPELECTION_WRONG_TIMESTAMP)
  private final val paramsWithSetupElectionWrongTimestampOrder: ParamsWithMessage = new ParamsWithMessage(rightElectionChannel, MESSAGE_SETUPELECTION_WRONG_ORDER)
  private final val paramsWithSetupElectionWrongId: ParamsWithMessage = new ParamsWithMessage(rightElectionChannel, MESSAGE_SETUPELECTION_WRONG_ID)
  private final val paramsWithSetupElectionWrongTimestampOrder2: ParamsWithMessage = new ParamsWithMessage(rightElectionChannel, MESSAGE_SETUPELECTION_WRONG_ORDER2)
  private final val paramsWithSetupElectionWrongOwner: ParamsWithMessage = new ParamsWithMessage(rightElectionChannel, MESSAGE_SETUPELECTION_WRONG_OWNER)
  final val SETUP_ELECTION_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithSetupElection, id)
  final val SETUP_ELECTION_WRONG_TIMESTAMP_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithSetupElectionWrongTimestamp, id)
  final val SETUP_ELECTION_WRONG_TIMESTAMP_ORDER_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithSetupElectionWrongTimestampOrder, id)
  final val SETUP_ELECTION_WRONG_TIMESTAMP_ORDER_RPC2: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithSetupElectionWrongTimestampOrder2, id)
  final val SETUP_ELECTION_WRONG_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithSetupElectionWrongId, id)
  final val SETUP_ELECTION_WRONG_OWNER_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithSetupElectionWrongOwner, id)

  //For OpenElection testing
  private final val electionChannel: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("laoId") + Channel.CHANNEL_SEPARATOR + Base64Data.encode("election"))
  private final val paramsWithOpenElection: ParamsWithMessage = new ParamsWithMessage(electionChannel, MESSAGE_OPEN_ELECTION_WORKING)
  private final val paramsWithOpenElectionWrongTimestamp: ParamsWithMessage = new ParamsWithMessage(electionChannel, MESSAGE_OPEN_ELECTION_WRONG_TIMESTAMP)
  private final val paramsWithOpenElectionWrongId: ParamsWithMessage = new ParamsWithMessage(electionChannel, MESSAGE_OPEN_ELECTION_WRONG_ID)
  private final val paramsWithOpenElectionWrongLaoId: ParamsWithMessage = new ParamsWithMessage(electionChannel, MESSAGE_OPEN_ELECTION_WRONG_LAO_ID)
  private final val paramsWithOpenElectionWrongOwner: ParamsWithMessage = new ParamsWithMessage(electionChannel, MESSAGE_OPEN_ELECTION_WRONG_OWNER)
  final val OPEN_ELECTION_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithOpenElection, id)
  final val OPEN_ELECTION_WRONG_TIMESTAMP_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithOpenElectionWrongTimestamp, id)
  final val OPEN_ELECTION_WRONG_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithOpenElectionWrongId, id)
  final val OPEN_ELECTION_WRONG_LAO_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithOpenElectionWrongLaoId, id)
  final val OPEN_ELECTION_WRONG_OWNER_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithOpenElectionWrongOwner, id)

  //For CastVoteElection testing
  private final val paramsWithCastVoteElection: ParamsWithMessage = new ParamsWithMessage(electionChannel, MESSAGE_CAST_VOTE_ELECTION_WORKING)
  private final val paramsWithCastVoteElectionWrongTimestamp: ParamsWithMessage = new ParamsWithMessage(electionChannel, MESSAGE_CAST_VOTE_ELECTION_WRONG_TIMESTAMP)
  private final val paramsWithCastVoteElectionWrongId: ParamsWithMessage = new ParamsWithMessage(electionChannel, MESSAGE_CAST_VOTE_ELECTION_WRONG_ID)
  private final val paramsWithCastVoteElectionWrongLaoId: ParamsWithMessage = new ParamsWithMessage(electionChannel, MESSAGE_CAST_VOTE_ELECTION_WRONG_LAO_ID)
  private final val paramsWithCastVoteElectionWrongOwner: ParamsWithMessage = new ParamsWithMessage(electionChannel, MESSAGE_CAST_VOTE_ELECTION_WRONG_OWNER)
  private final val paramsWithCastVoteElectionInvalidVote: ParamsWithMessage = new ParamsWithMessage(electionChannel, MESSAGE_CAST_VOTE_INVALID_VOTES)
  private final val paramsWithCastVoteElectionInvalidBallot: ParamsWithMessage = new ParamsWithMessage(electionChannel, MESSAGE_CAST_VOTE_INVALID_BALLOT)
  final val CAST_VOTE_ELECTION_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCastVoteElection, id)
  final val CAST_VOTE_ELECTION_WRONG_TIMESTAMP_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCastVoteElectionWrongTimestamp, id)
  final val CAST_VOTE_ELECTION_WRONG_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCastVoteElectionWrongId, id)
  final val CAST_VOTE_ELECTION_WRONG_LAO_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCastVoteElectionWrongLaoId, id)
  final val CAST_VOTE_ELECTION_WRONG_OWNER_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCastVoteElectionWrongOwner, id)
  final val CAST_VOTE_ELECTION_INVALID_VOTE_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCastVoteElectionInvalidVote, id)
  final val CAST_VOTE_ELECTION_INVALID_BALLOT_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCastVoteElectionInvalidBallot, id)

  //For EndElection testing
  private final val paramsWithEndElection: ParamsWithMessage = new ParamsWithMessage(electionChannel, MESSAGE_END_ELECTION_WORKING)
  private final val paramsWithEndElectionWrongTimestamp: ParamsWithMessage = new ParamsWithMessage(electionChannel, MESSAGE_END_ELECTION_WRONG_TIMESTAMP)
  private final val paramsWithEndElectionWrongId: ParamsWithMessage = new ParamsWithMessage(electionChannel, MESSAGE_END_ELECTION_WRONG_ID)
  private final val paramsWithEndElectionWrongLaoId: ParamsWithMessage = new ParamsWithMessage(electionChannel, MESSAGE_END_ELECTION_WRONG_LAO_ID)
  private final val paramsWithEndElectionWrongOwner: ParamsWithMessage = new ParamsWithMessage(electionChannel, MESSAGE_END_ELECTION_WRONG_OWNER)
  final val END_ELECTION_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithEndElection, id)
  final val END_ELECTION_WRONG_TIMESTAMP_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithEndElectionWrongTimestamp, id)
  final val END_ELECTION_WRONG_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithEndElectionWrongId, id)
  final val END_ELECTION_WRONG_LAO_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithEndElectionWrongLaoId, id)
  final val END_ELECTION_WRONG_OWNER_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithEndElectionWrongOwner, id)

  // broadcast JsonRpcRequest
  final val broadcastRpcRequest: JsonRpcRequest = JsonRpcRequest(rpc, MethodType.BROADCAST, paramsWithMessage, None)
}
