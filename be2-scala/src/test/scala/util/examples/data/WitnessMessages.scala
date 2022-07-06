package util.examples.data

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.ActionType
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.objects.{Base64Data, Channel}
import util.examples.data.traits.WitnessMessagesTrait

/** Generates high level Election Messages from protocol folder For content validation: all the params are required
  */
object WitnessMessages extends WitnessMessagesTrait {

  override val action: ActionType = ActionType.WITNESS
  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("witness_message_channel"))

  final val witnessMessage: JsonRpcRequest = getJsonRPCRequestFromFile("message_witness.json")()

}
