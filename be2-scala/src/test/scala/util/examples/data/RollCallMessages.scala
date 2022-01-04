package util.examples.data

import ch.epfl.pop.model.network.method.message.data.{ObjectType, ActionType}
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.MethodType
import ch.epfl.pop.model.network.method.ParamsWithMessage
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.rollCall.CreateRollCall
import ch.epfl.pop.model.network.requests.rollCall.JsonRpcRequestCreateRollCall

import ch.epfl.pop.model.objects.{Channel, Signature, Base64Data, PublicKey, Hash, WitnessSignaturePair}
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import util.examples.dataTraits.RollCallMessagesTrait

import java.nio.file.{Files, Path}

/**
  * Generates high level RollCall Messages from protocol folder
  * For content validation: all the params are required
  * For handling: id, message with decoded data, channel are required
  */
object CreateRollCallMessages extends RollCallMessagesTrait {

  override val action  = ActionType.CREATE
  override val CHANNEL = Channel(Channel.ROOT_CHANNEL_PREFIX + "create_roll_call_channel")

  final val createRollCall: JsonRpcRequest = getJsonRPCRequestFromFile("roll_call_create.json")()

  //TODO: Generate other Create RollCall messages
}


object OpenRollCallMessages extends RollCallMessagesTrait {

  override val action = ActionType.CREATE

  override val CHANNEL = Channel(Channel.ROOT_CHANNEL_PREFIX + "open_roll_call_channel")

  final val openRollCall: JsonRpcRequest =  getJsonRPCRequestFromFile("roll_call_open.json")()

  //TODO: Generate other Open RollCall messages
}

object CloseRollCallMessages extends RollCallMessagesTrait {

  override val action = ActionType.CLOSE

  override val CHANNEL = Channel(Channel.ROOT_CHANNEL_PREFIX + "close_roll_call_channel")

  final val openRollCall: JsonRpcRequest =  getJsonRPCRequestFromFile("roll_call_close.json")()

  //TODO: Generate other Clsoe RollCall messages
}

object ReopenRollCallMessages extends RollCallMessagesTrait {

  override val action = ActionType.REOPEN

  override val CHANNEL = Channel(Channel.ROOT_CHANNEL_PREFIX + "reopen_roll_call_channel")

  final val openRollCall: JsonRpcRequest =  getJsonRPCRequestFromFile("reopen_call_close.json")()

  //TODO: Generate other Clsoe RollCall messages
}

object RollCallCustomBuilders {
  //TODO: Add here custom highlevel message builders to forge data
}
