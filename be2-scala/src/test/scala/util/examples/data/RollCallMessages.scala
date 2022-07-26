package util.examples.data

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.ActionType
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.objects.Channel
import util.examples.data.traits.RollCallMessagesTrait

/** Generates high level RollCall Messages from protocol folder For content validation: all the params are required For handling: id, message with decoded data, channel are required
  */
object CreateRollCallMessages extends RollCallMessagesTrait {

  override val action: ActionType = ActionType.CREATE
  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + "create_roll_call_channel")

  final val createRollCall: JsonRpcRequest = getJsonRPCRequestFromFile("roll_call_create.json")()

  // TODO: Generate other Create RollCall messages
}

object OpenRollCallMessages extends RollCallMessagesTrait {

  override val action: ActionType = ActionType.OPEN

  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + "open_roll_call_channel")

  final val openRollCall: JsonRpcRequest = getJsonRPCRequestFromFile("roll_call_open.json")()

  // TODO: Generate other Open RollCall messages
}

object CloseRollCallMessages extends RollCallMessagesTrait {

  override val action: ActionType = ActionType.CLOSE

  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + "close_roll_call_channel")

  final val openRollCall: JsonRpcRequest = getJsonRPCRequestFromFile("roll_call_close.json")()

  // TODO: Generate other Close RollCall messages
}

object ReopenRollCallMessages extends RollCallMessagesTrait {

  override val action: ActionType = ActionType.REOPEN

  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + "reopen_roll_call_channel")

  final val openRollCall: JsonRpcRequest = getJsonRPCRequestFromFile("reopen_call_close.json")()

  // TODO: Generate other Close RollCall messages
}

object RollCallCustomBuilders {
  // TODO: Add here custom highlevel message builders to forge data
}
