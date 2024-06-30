package util.examples.data

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.ActionType
import ch.epfl.pop.model.objects.{Base64Data, Channel}
import util.examples.data.traits.FederationMessagesTrait

/** Generates high level Federation Messages from protocol folder For content validation: all the params are required
  */
object FederationChallengeRequestMessages extends FederationMessagesTrait {

  override val action: ActionType = ActionType.challenge_request
  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("challenge_request_channel"))

  final val federationChallengeRequest: JsonRpcRequest = getJsonRPCRequestFromFile("federation_challenge_request/federation_challenge_request.json")()

}

object FederationExpectMessages extends FederationMessagesTrait {

  override val action: ActionType = ActionType.expect
  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("expect_channel"))

  final val federationExpect: JsonRpcRequest = getJsonRPCRequestFromFile("federation_expect/federation_expect.json")()

}

object FederationInitMessages extends FederationMessagesTrait {

  override val action: ActionType = ActionType.init
  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("init_channel"))

  final val federationInit: JsonRpcRequest = getJsonRPCRequestFromFile("federation_init/federation_init.json")()

}

object FederationChallengeMessages extends FederationMessagesTrait {

  override val action: ActionType = ActionType.challenge
  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("challenge_channel"))

  final val federationChallenge: JsonRpcRequest = getJsonRPCRequestFromFile("federation_challenge/federation_challenge.json")()

}
object FederationResultMessages extends FederationMessagesTrait {

  override val action: ActionType = ActionType.result
  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("result_channel"))

  final val federationResult: JsonRpcRequest = getJsonRPCRequestFromFile("federation_result/federation_result.json")()

}

object FederationTokensExchangeMessages extends FederationMessagesTrait {

  override val action: ActionType = ActionType.tokens_exchange
  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("tokens_exchange_channel"))

  final val federationTokensExchange: JsonRpcRequest = getJsonRPCRequestFromFile("federation_tokens_exchange/federation_tokens_exchange.json")()
}
