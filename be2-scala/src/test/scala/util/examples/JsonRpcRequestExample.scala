package util.examples

import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.{GetMessagesById, Heartbeat, ParamsWithChannel, ParamsWithMessage}
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse, MethodType, ResultObject}
import ch.epfl.pop.model.objects.{Base64Data, Channel, Hash}
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import util.examples.Election.CastVoteElectionExamples._
import util.examples.Election.OpenElectionExamples._
import util.examples.Election.SetupElectionExamples.{ELECTION_ID, _}
import util.examples.Election.EndElectionExamples._
import util.examples.Election.KeyElectionExamples._
import util.examples.Lao.GreetLaoExamples._
import util.examples.MessageExample._
import util.examples.Witness.WitnessMessageExamples._
import util.examples.RollCall.CloseRollCallExamples._
import util.examples.RollCall.CreateRollCallExamples._
import util.examples.RollCall.OpenRollCallExamples._
import util.examples.socialMedia.AddChirpExamples._
import util.examples.socialMedia.AddReactionExamples._
import util.examples.socialMedia.DeleteChirpExamples._
import util.examples.socialMedia.DeleteReactionExamples._

object JsonRpcRequestExample {

  private final val rpc: String = "rpc"
  private final val id: Option[Int] = Some(0)
  private final val methodType: MethodType.MethodType = MethodType.PUBLISH
  private final val channel: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + "channel")
  private final val paramsWithoutMessage: ParamsWithChannel = new ParamsWithChannel(channel)
  private final val paramsWithMessage: ParamsWithMessage = new ParamsWithMessage(channel, MESSAGE_WORKING_WS_PAIR)
  private final val paramsWithChannel: ParamsWithChannel = new ParamsWithChannel(channel)
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

  // for GreetLao testing
  private final val laoChannel: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("laoId"))
  private final val electionChannelGreet: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("laoId") + Channel.CHANNEL_SEPARATOR + Base64Data.encode("election"))
  private final val paramsWithGreetLao: ParamsWithMessage = new ParamsWithMessage(laoChannel, MESSAGE_GREET_LAO)
  private final val paramsWithGreetLaoWrongFrontend: ParamsWithMessage = new ParamsWithMessage(laoChannel, MESSAGE_GREET_LAO_WRONG_FRONTEND)
  private final val paramsWithGreetLaoWrongAddress: ParamsWithMessage = new ParamsWithMessage(laoChannel, MESSAGE_GREET_LAO_WRONG_ADDRESS)
  private final val paramsWithGreetLaoWrongLao: ParamsWithMessage = new ParamsWithMessage(laoChannel, MESSAGE_GREET_LAO_WRONG_LAO)
  private final val paramsWithGreetLaoWrongSender: ParamsWithMessage = new ParamsWithMessage(laoChannel, MESSAGE_GREET_LAO_WRONG_OWNER)
  private final val paramsWithGreetLaoWrongChannel: ParamsWithMessage = new ParamsWithMessage(electionChannelGreet, MESSAGE_GREET_LAO_WRONG_CHANNEL)
  final val GREET_LAO_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithGreetLao, id)
  final val GREET_LAO_WRONG_FRONTEND_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithGreetLaoWrongFrontend, id)
  final val GREET_LAO_WRONG_ADDRESS_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithGreetLaoWrongAddress, id)
  final val GREET_LAO_WRONG_LAO_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithGreetLaoWrongLao, id)
  final val GREET_LAO_WRONG_SENDER_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithGreetLaoWrongSender, id)
  final val GREET_LAO_WRONG_CHANNEL_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithGreetLaoWrongChannel, id)

  // for CreateRollCall testing
  private final val rollCallChannel: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("laoId"))
  private final val paramsWithCreateRollCall: ParamsWithMessage = new ParamsWithMessage(rollCallChannel, MESSAGE_CREATE_ROLL_CALL_WORKING)
  private final val paramsWithCreateRollCallWrongTimestamp: ParamsWithMessage = new ParamsWithMessage(rollCallChannel, MESSAGE_CREATE_ROLL_CALL_WRONG_TIMESTAMP)
  private final val paramsWithCreateRollCallWrongTimestampOrder: ParamsWithMessage = new ParamsWithMessage(rollCallChannel, MESSAGE_CREATE_ROLL_CALL_WRONG_TIMESTAMP_ORDER)
  private final val paramsWithCreateRollCallWrongId: ParamsWithMessage = new ParamsWithMessage(rollCallChannel, MESSAGE_CREATE_ROLL_CALL_WRONG_ID)
  private final val paramsWithCreateRollCallWrongSender: ParamsWithMessage = new ParamsWithMessage(rollCallChannel, MESSAGE_CREATE_ROLL_CALL_WRONG_SENDER)
  final val CREATE_ROLL_CALL_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateRollCall, id)
  final val CREATE_ROLL_CALL_WRONG_TIMESTAMP_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateRollCallWrongTimestamp, id)
  final val CREATE_ROLL_CALL_WRONG_TIMESTAMP_ORDER_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateRollCallWrongTimestampOrder, id)
  final val CREATE_ROLL_CALL_WRONG_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateRollCallWrongId, id)
  final val CREATE_ROLL_CALL_WRONG_SENDER_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateRollCallWrongSender, id)

  // for OpenRollCall testing
  private final val paramsWithOpenRollCall: ParamsWithMessage = new ParamsWithMessage(rollCallChannel, MESSAGE_OPEN_ROLL_CALL_WORKING)
  private final val paramsWithOpenRollCallWrongTimestamp: ParamsWithMessage = new ParamsWithMessage(rollCallChannel, MESSAGE_OPEN_ROLL_CALL_WRONG_TIMESTAMP)
  private final val paramsWithOpenRollCallWrongId: ParamsWithMessage = new ParamsWithMessage(rollCallChannel, MESSAGE_OPEN_ROLL_CALL_WRONG_ID)
  private final val paramsWithOpenRollCallWrongSender: ParamsWithMessage = new ParamsWithMessage(rollCallChannel, MESSAGE_OPEN_ROLL_CALL_WRONG_SENDER)
  private final val paramsWithOpenRollCallWrongOpens: ParamsWithMessage = new ParamsWithMessage(rollCallChannel, MESSAGE_OPEN_ROLL_CALL_WRONG_OPENS)
  private final val paramsWithOpenRollCallValidOpens: ParamsWithMessage = new ParamsWithMessage(rollCallChannel, MESSAGE_OPEN_ROLL_CALL_VALID_OPENS)
  final val OPEN_ROLL_CALL_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithOpenRollCall, id)
  final val OPEN_ROLL_CALL_WRONG_TIMESTAMP_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithOpenRollCallWrongTimestamp, id)
  final val OPEN_ROLL_CALL_WRONG_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithOpenRollCallWrongId, id)
  final val OPEN_ROLL_CALL_WRONG_SENDER_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithOpenRollCallWrongSender, id)
  final val OPEN_ROLL_CALL_WRONG_OPENS_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithOpenRollCallWrongOpens, id)
  final val OPEN_ROLL_CALL_VALID_OPENS_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithOpenRollCallValidOpens, id)

  // for CloseRollCall testing
  private final val paramsWithCloseRollCall: ParamsWithMessage = new ParamsWithMessage(rollCallChannel, MESSAGE_CLOSE_ROLL_CALL_WORKING)
  private final val paramsWithCloseRollCallWrongTimestamp: ParamsWithMessage = new ParamsWithMessage(rollCallChannel, MESSAGE_CLOSE_ROLL_CALL_WRONG_TIMESTAMP)
  private final val paramsWithCloseRollCallWrongId: ParamsWithMessage = new ParamsWithMessage(rollCallChannel, MESSAGE_CLOSE_ROLL_CALL_WRONG_ID)
  private final val paramsWithCloseRollCallWrongAttendees: ParamsWithMessage = new ParamsWithMessage(rollCallChannel, MESSAGE_CLOSE_ROLL_CALL_WRONG_ATTENDEES)
  private final val paramsWithCloseRollCallWrongDuplicateAttendees: ParamsWithMessage = new ParamsWithMessage(rollCallChannel, MESSAGE_CLOSE_ROLL_CALL_WRONG_DUPLICATE_ATTENDEES)
  private final val paramsWithCloseRollCallAlreadyClosed: ParamsWithMessage = new ParamsWithMessage(rollCallChannel, MESSAGE_CLOSE_ROLL_CALL_ALREADY_CLOSED)
  private final val paramsWithCloseRollCallWrongSender: ParamsWithMessage = new ParamsWithMessage(rollCallChannel, MESSAGE_CLOSE_ROLL_CALL_WRONG_SENDER)
  private final val paramsWithCloseRollCallWrongCloses: ParamsWithMessage = new ParamsWithMessage(rollCallChannel, MESSAGE_CLOSE_ROLL_CALL_WRONG_CLOSES)
  final val CLOSE_ROLL_CALL_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCloseRollCall, id)
  final val CLOSE_ROLL_CALL_WRONG_TIMESTAMP_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCloseRollCallWrongTimestamp, id)
  final val CLOSE_ROLL_CALL_WRONG_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCloseRollCallWrongId, id)
  final val CLOSE_ROLL_CALL_WRONG_ATTENDEES_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCloseRollCallWrongAttendees, id)
  final val CLOSE_ROLL_CALL_WRONG_DUPLICATE_ATTENDEES_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCloseRollCallWrongDuplicateAttendees, id)
  final val CLOSE_ROLL_CALL_ALREADY_CLOSED_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCloseRollCallAlreadyClosed, id)
  final val CLOSE_ROLL_CALL_WRONG_SENDER_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCloseRollCallWrongSender, id)
  final val CLOSE_ROLL_CALL_WRONG_CLOSES_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCloseRollCallWrongCloses, id)

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
  private final val paramsWithSetupElection: ParamsWithMessage = new ParamsWithMessage(rightElectionChannel, MESSAGE_SETUPELECTION_OPEN_BALLOT_WORKING)
  private final val paramsWithSetupElectionSecretBallot: ParamsWithMessage = new ParamsWithMessage(rightElectionChannel, MESSAGE_SETUPELECTION_SECRET_BALLOT_WORKING)
  private final val paramsWithSetupElectionWrongTimestamp: ParamsWithMessage = new ParamsWithMessage(rightElectionChannel, MESSAGE_SETUPELECTION_WRONG_TIMESTAMP)
  private final val paramsWithSetupElectionWrongTimestampOrder: ParamsWithMessage = new ParamsWithMessage(rightElectionChannel, MESSAGE_SETUPELECTION_WRONG_ORDER)
  private final val paramsWithSetupElectionWrongId: ParamsWithMessage = new ParamsWithMessage(rightElectionChannel, MESSAGE_SETUPELECTION_WRONG_ID)
  private final val paramsWithSetupElectionWrongTimestampOrder2: ParamsWithMessage = new ParamsWithMessage(rightElectionChannel, MESSAGE_SETUPELECTION_WRONG_ORDER2)
  private final val paramsWithSetupElectionWrongOwner: ParamsWithMessage = new ParamsWithMessage(rightElectionChannel, MESSAGE_SETUPELECTION_WRONG_OWNER)
  private final val paramsWithSetupElectionWrongQuestionId: ParamsWithMessage = new ParamsWithMessage(rightElectionChannel, MESSAGE_SETUPELECTION_WRONG_QUESTION_ID)
  final val SETUP_ELECTION_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithSetupElection, id)
  final val SETUP_ELECTION_SECRET_BALLOT_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithSetupElectionSecretBallot, id)
  final val SETUP_ELECTION_WRONG_TIMESTAMP_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithSetupElectionWrongTimestamp, id)
  final val SETUP_ELECTION_WRONG_TIMESTAMP_ORDER_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithSetupElectionWrongTimestampOrder, id)
  final val SETUP_ELECTION_WRONG_TIMESTAMP_ORDER_RPC2: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithSetupElectionWrongTimestampOrder2, id)
  final val SETUP_ELECTION_WRONG_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithSetupElectionWrongId, id)
  final val SETUP_ELECTION_WRONG_OWNER_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithSetupElectionWrongOwner, id)
  final val SETUP_ELECTION_WRONG_QUESTION_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithSetupElectionWrongQuestionId, id)

  // For OpenElection testing
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

  // For KeyElection testing
  private final val paramsWithKeyElection: ParamsWithMessage = new ParamsWithMessage(electionChannel, MESSAGE_KEY_ELECTION_WORKING)
  private final val paramsWithKeyElectionWrongElectionId: ParamsWithMessage = new ParamsWithMessage(electionChannel, MESSAGE_KEY_ELECTION_WRONG_ELECTION_ID)
  private final val paramsWithKeyElectionWrongOwner: ParamsWithMessage = new ParamsWithMessage(electionChannel, MESSAGE_KEY_ELECTION_WRONG_OWNER)
  final val KEY_ELECTION_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithKeyElection, id)
  final val KEY_ELECTION_WRONG_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithKeyElectionWrongElectionId, id)
  final val KEY_ELECTION_WRONG_OWNER_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithKeyElectionWrongOwner, id)

  // For CastVoteElection testing
  private final val electionChannelCastVote: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("laoId") + Channel.CHANNEL_SEPARATOR + ELECTION_ID)
  private final val paramsWithCastVoteElection: ParamsWithMessage = new ParamsWithMessage(electionChannelCastVote, MESSAGE_CAST_VOTE_ELECTION_WORKING)
  private final val paramsWithCastVoteElectionWrongTimestamp: ParamsWithMessage = new ParamsWithMessage(electionChannelCastVote, MESSAGE_CAST_VOTE_ELECTION_WRONG_TIMESTAMP)
  private final val paramsWithCastVoteElectionWrongId: ParamsWithMessage = new ParamsWithMessage(electionChannelCastVote, MESSAGE_CAST_VOTE_ELECTION_WRONG_ID)
  private final val paramsWithCastVoteElectionWrongLaoId: ParamsWithMessage = new ParamsWithMessage(electionChannelCastVote, MESSAGE_CAST_VOTE_ELECTION_WRONG_LAO_ID)
  private final val paramsWithCastVoteElectionWrongOwner: ParamsWithMessage = new ParamsWithMessage(electionChannelCastVote, MESSAGE_CAST_VOTE_ELECTION_WRONG_OWNER)
  private final val paramsWithCastVoteElectionInvalidVote: ParamsWithMessage = new ParamsWithMessage(electionChannelCastVote, MESSAGE_CAST_VOTE_INVALID_VOTES)
  private final val paramsWithCastVoteElectionInvalidBallot: ParamsWithMessage = new ParamsWithMessage(electionChannelCastVote, MESSAGE_CAST_VOTE_INVALID_BALLOT)
  private final val paramsWithCastVoteElectionInvalidVoteId: ParamsWithMessage = new ParamsWithMessage(electionChannelCastVote, MESSAGE_CAST_VOTE_INVALID_VOTE_ID)
  final val CAST_VOTE_ELECTION_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCastVoteElection, id)
  final val CAST_VOTE_ELECTION_WRONG_TIMESTAMP_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCastVoteElectionWrongTimestamp, id)
  final val CAST_VOTE_ELECTION_WRONG_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCastVoteElectionWrongId, id)
  final val CAST_VOTE_ELECTION_WRONG_LAO_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCastVoteElectionWrongLaoId, id)
  final val CAST_VOTE_ELECTION_WRONG_OWNER_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCastVoteElectionWrongOwner, id)
  final val CAST_VOTE_ELECTION_INVALID_VOTE_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCastVoteElectionInvalidVote, id)
  final val CAST_VOTE_ELECTION_INVALID_BALLOT_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCastVoteElectionInvalidBallot, id)
  final val CAST_VOTE_ELECTION_INVALID_VOTE_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCastVoteElectionInvalidVoteId, id)

  // For EndElection testing
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

  // for WitnessMessage testing
  private final val paramsWithWitnessMessage: ParamsWithMessage = new ParamsWithMessage(laoChannel, MESSAGE_WITNESS_MESSAGE_WORKING)
  private final val paramsWithWitnessMessageWrongSignature: ParamsWithMessage = new ParamsWithMessage(laoChannel, MESSAGE_WITNESS_MESSAGE_WRONG_SIGNATURE)
  private final val paramsWithWitnessMessageWrongOwner: ParamsWithMessage = new ParamsWithMessage(laoChannel, MESSAGE_WITNESS_MESSAGE_WRONG_OWNER)
  final val WITNESS_MESSAGE_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithWitnessMessage, id)
  final val WITNESS_MESSAGE_WRONG_SIGNATURE_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithWitnessMessageWrongSignature, id)
  final val WITNESS_MESSAGE_WRONG_SENDER_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithWitnessMessageWrongOwner, id)

  // For Meeting testing
  private final val laoChannelMeeting: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + "p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=")
  private final val laoChannelMeetingWrong: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + "wrongMeetingChannel/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=")
  private final val paramsWithCreateMeeting: ParamsWithMessage = new ParamsWithMessage(laoChannelMeeting, MESSAGE_CREATE_MEETING)
  private final val paramsWithCreateMeetingWrongChannel: ParamsWithMessage = new ParamsWithMessage(laoChannelMeetingWrong, MESSAGE_CREATE_MEETING_WRONG_CHANNEL)
  private final val paramsWithWrongDataCreateMeeting: ParamsWithMessage = new ParamsWithMessage(laoChannelMeeting, MESSAGE_CREATE_MEETING_WRONG_DATA)
  private final val paramsWithCreateMeetingInvalidCreation: ParamsWithMessage = new ParamsWithMessage(laoChannelMeeting, MESSAGE_CREATE_MEETING_SMALL_CREATION)
  private final val paramsWithCreateMeetingInvalidStart: ParamsWithMessage = new ParamsWithMessage(laoChannelMeeting, MESSAGE_CREATE_MEETING_SMALL_START)
  private final val paramsWithCreateMeetingInvalidStartEnd: ParamsWithMessage = new ParamsWithMessage(laoChannelMeeting, MESSAGE_CREATE_MEETING_START_BIGGER_THAN_END)
  private final val paramsWithCreateMeetingInvalidEnd: ParamsWithMessage = new ParamsWithMessage(laoChannelMeeting, MESSAGE_CREATE_MEETING_SMALL_END)
  private final val paramsWithStateMeetingValid: ParamsWithMessage = new ParamsWithMessage(laoChannelMeeting, MESSAGE_STATE_MEETING)
  private final val paramsWithStateMeetingInvalidData: ParamsWithMessage = new ParamsWithMessage(laoChannel, MESSAGE_STATE_MEETING_INVALID_DATA)
  private final val paramsWithStateMeetingInvalidCreation: ParamsWithMessage = new ParamsWithMessage(laoChannel, MESSAGE_STATE_MEETING_INVALID_CREATION)
  private final val paramsWithStateMeetingInvalidStart: ParamsWithMessage = new ParamsWithMessage(laoChannel, MESSAGE_STATE_MEETING_INVALID_START)
  private final val paramsWithStateMeetingSmallStart: ParamsWithMessage = new ParamsWithMessage(laoChannel, MESSAGE_STATE_MEETING_SMALL_START)
  private final val paramsWithStateMeetingSmallEnd: ParamsWithMessage = new ParamsWithMessage(laoChannel, MESSAGE_STATE_MEETING_SMALL_END)
  private final val paramsWithStateMeetingBigStart: ParamsWithMessage = new ParamsWithMessage(laoChannel, MESSAGE_STATE_MEETING_BIG_START)
  private final val paramsWithStateMeetingWrongWitness: ParamsWithMessage = new ParamsWithMessage(laoChannel, MESSAGE_STATE_MEETING_WRONG_WITNESS_SIGNATURE)
  private final val paramsWithStateMeetingSmallModificationTime: ParamsWithMessage = new ParamsWithMessage(laoChannel, MESSAGE_STATE_MEETING_SMALL_MODIFICATION_TIME)
  final val CREATE_MEETING_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateMeeting, id)
  final val CREATE_MEETING_WRONG_CHANNEL_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateMeetingWrongChannel, id)
  final val CREATE_MEETING_INVALID_DATA_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithWrongDataCreateMeeting, id)
  final val CREATE_MEETING_INVALID_CREATION_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateMeetingInvalidCreation, id)
  final val CREATE_MEETING_INVALID_START_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateMeetingInvalidStart, id)
  final val CREATE_MEETING_INVALID_STARTEND_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateMeetingInvalidStartEnd, id)
  final val CREATE_MEETING_INVALID_END_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateMeetingInvalidEnd, id)
  final val STATE_MEETING_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithStateMeetingValid, id)
  final val STATE_MEETING_INVALID_DATA_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithStateMeetingInvalidData, id)
  final val STATE_MEETING_INVALID_CREATION_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithStateMeetingInvalidCreation, id)
  final val STATE_MEETING_INVALID_START_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithStateMeetingInvalidStart, id)
  final val STATE_MEETING_SMALL_START_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithStateMeetingSmallStart, id)
  final val STATE_MEETING_SMALL_END_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithStateMeetingSmallEnd, id)
  final val STATE_MEETING_BIG_START_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithStateMeetingBigStart, id)
  final val STATE_MEETING_WRONGWITNESS_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithStateMeetingWrongWitness, id)
  final val STATE_MEETING_SMALLMODIFICATION_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithStateMeetingSmallModificationTime, id)

  // broadcast JsonRpcRequest
  final val broadcastRpcRequest: JsonRpcRequest = JsonRpcRequest(rpc, MethodType.BROADCAST, paramsWithMessage, None)

  // paramsWithChannel JsonRpcRequest
  final val subscribeRpcRequest: JsonRpcRequest = JsonRpcRequest(rpc, MethodType.SUBSCRIBE, paramsWithChannel, id)
  final val unSubscribeRpcRequest: JsonRpcRequest = JsonRpcRequest(rpc, MethodType.UNSUBSCRIBE, paramsWithChannel, id)
  final val catchupRpcRequest: JsonRpcRequest = JsonRpcRequest(rpc, MethodType.CATCHUP, paramsWithChannel, id)

  // paramsWithMap JsonRpcRequest
  // defining the channels
  final val CHANNEL1_NAME: String = "/root/wex/lao1Id"
  final val CHANNEL2_NAME: String = "/root/wex/lao2Id"
  final val CHANNEL1 = new Channel(CHANNEL1_NAME)
  final val CHANNEL2 = new Channel(CHANNEL2_NAME)

  // defining the messages
  final val MESSAGE1_ID: Hash = Hash(Base64Data.encode("message1Id"))
  final val MESSAGE2_ID: Hash = Hash(Base64Data.encode("message2Id"))
  final val MESSAGE3_ID: Hash = Hash(Base64Data.encode("message3Id"))
  final val MESSAGE4_ID: Hash = Hash(Base64Data.encode("message4Id"))
  final val MESSAGE5_ID: Hash = Hash(Base64Data.encode("message5Id"))
  final val MESSAGE1: Message = Message(null, null, null, MESSAGE1_ID, null, null)
  final val MESSAGE2: Message = Message(null, null, null, MESSAGE2_ID, null, null)
  final val MESSAGE3: Message = Message(null, null, null, MESSAGE3_ID, null, null)
  final val MESSAGE4: Message = Message(null, null, null, MESSAGE4_ID, null, null)
  final val MESSAGE5: Message = Message(null, null, null, MESSAGE5_ID, null, null)

  // defining a received heartbeat
  final val RECEIVED_HEARTBEAT_PARAMS = Map(CHANNEL1 -> Set(MESSAGE1_ID, MESSAGE2_ID, MESSAGE3_ID), CHANNEL2 -> Set(MESSAGE4_ID, MESSAGE5_ID))
  final val RECEIVED_HEARTBEAT: Heartbeat = Heartbeat(RECEIVED_HEARTBEAT_PARAMS)
  final val VALID_RECEIVED_HEARTBEAT_RPC: JsonRpcRequest = JsonRpcRequest(rpc, MethodType.HEARTBEAT, RECEIVED_HEARTBEAT, id)

  // defining what the answer to the received heartbeat should be
  final val EXPECTED_MISSING_MESSAGE_IDS = Map(CHANNEL1 -> Set(MESSAGE2_ID, MESSAGE3_ID), CHANNEL2 -> Set(MESSAGE5_ID))
  final val EXPECTED_GET_MSGS_BY_ID_RESPONSE: GetMessagesById = GetMessagesById(EXPECTED_MISSING_MESSAGE_IDS)
  final val EXPECTED_GET_MSGS_BY_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, MethodType.GET_MESSAGES_BY_ID, EXPECTED_GET_MSGS_BY_ID_RESPONSE, id)

  // defining a received getMsgsById
  final val RECEIVED_GET_MSG_BY_ID_PARAMS = Map(CHANNEL1 -> Set(MESSAGE1_ID))
  final val RECEIVED_GET_MSG_BY_ID: GetMessagesById = GetMessagesById(RECEIVED_GET_MSG_BY_ID_PARAMS)
  final val VALID_RECEIVED_GET_MSG_BY_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, MethodType.GET_MESSAGES_BY_ID, RECEIVED_GET_MSG_BY_ID, id)

  // defining what the answer to the received getMsgsById should be
  final val EXPECTED_MISSING_MESSAGES = Map(CHANNEL1 -> Set(MESSAGE1))
  final val EXPECTED_GET_MSGS_BY_ID_RPC_RESPONSE: JsonRpcResponse = JsonRpcResponse(RpcValidator.JSON_RPC_VERSION, Some(new ResultObject(EXPECTED_MISSING_MESSAGES)), None, None)

}
