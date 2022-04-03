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

  //TODO: Generate other Set Up Election messages
}


object OpenElectionMessages extends ElectionMessagesTrait {

  override val action: ActionType = ActionType.OPEN

  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("open_election_channel"))

  final val openElection: JsonRpcRequest = getJsonRPCRequestFromFile("election_open.json")()

  //TODO: Generate other Open Election messages
}
