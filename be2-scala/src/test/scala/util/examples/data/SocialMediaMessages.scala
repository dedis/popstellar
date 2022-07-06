package util.examples.data

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.ActionType
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.objects.{Base64Data, Channel}
import util.examples.data.traits.{SocialMediaChirpMessagesTrait, SocialMediaReactionMessagesTrait}

/** Generates high level RollCall Messages from protocol folder For content validation: all the params are required For handling: id, message with decoded data, channel are required
  */
object AddReactionMessages extends SocialMediaReactionMessagesTrait {

  override val action: ActionType = ActionType.ADD
  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("add_reaction_channel"))

  final val addReaction: JsonRpcRequest = getJsonRPCRequestFromFile("reaction_add/reaction_add.json")()

  // TODO: Generate other messages
}

object DeleteReactionMessages extends SocialMediaReactionMessagesTrait {

  override val action: ActionType = ActionType.DELETE
  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("delete_reaction_channel"))

  final val deleteReaction: JsonRpcRequest = getJsonRPCRequestFromFile("reaction_delete/reaction_delete.json")()

  // TODO: Generate other messages
}

object AddChirpMessages extends SocialMediaChirpMessagesTrait {

  override val action: ActionType = ActionType.ADD

  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("add_chirp_channel"))

  final val addChirp: JsonRpcRequest = getJsonRPCRequestFromFile("chirp_add_publish/chirp_add_publish.json")()

  // TODO: Generate other messages
}

object DeleteChirpMessages extends SocialMediaChirpMessagesTrait {

  override val action: ActionType = ActionType.DELETE

  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("delete_chirp_channel"))

  final val deleteChirp: JsonRpcRequest = getJsonRPCRequestFromFile("chirp_delete_publish/chirp_delete_publish.json")()

  // TODO: Generate other messages
}
