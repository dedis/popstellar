package util.examples.data

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.ActionType
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.objects.{Base64Data, Channel}
import util.examples.data.traits.ElectionMessagesTrait

/**
 * Generates high level Election Messages from protocol folder
 * For content validation: all the params are required
 */
object SetupElectionMessages extends ElectionMessagesTrait {

  override val action: ActionType = ActionType.SETUP
  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("set_up_election_channel"))

  final val setupElection: JsonRpcRequest = getJsonRPCRequestFromFile("election_setup/election_setup.json")()

  final val setupElectionSecretBallot: JsonRpcRequest = getJsonRPCRequestFromFile("election_setup/election_setup_secret_ballot.json")()

  //TODO: Generate other Set Up Election messages
}


object OpenElectionMessages extends ElectionMessagesTrait {

  override val action: ActionType = ActionType.OPEN

  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("open_election_channel"))

  final val openElection: JsonRpcRequest = getJsonRPCRequestFromFile("election_open/election_open.json")()

  //TODO: Generate other Open Election messages
}

object KeyElectionMessages extends ElectionMessagesTrait {

  override val action: ActionType = ActionType.KEY

  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("key_election_channel"))

  final val keyElection: JsonRpcRequest = getJsonRPCRequestFromFile("election_key/election_key.json")()

}



object CastVoteElectionMessages extends ElectionMessagesTrait {

  override val action: ActionType = ActionType.CAST_VOTE

  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("cast_vote_election_channel"))

  final val castVoteElection: JsonRpcRequest = getJsonRPCRequestFromFile("vote_cast_vote/vote_cast_vote.json")()

  //TODO: Generate other CastVote Election messages
}

object EndElectionMessages extends ElectionMessagesTrait {

  override val action: ActionType = ActionType.END

  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("end_election_channel"))

  final val endElection: JsonRpcRequest = getJsonRPCRequestFromFile("election_end/election_end.json")()

  //TODO: Generate other Open Election messages
}
