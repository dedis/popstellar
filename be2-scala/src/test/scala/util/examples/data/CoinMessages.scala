package util.examples.data

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.ActionType
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.objects.Channel
import util.examples.data.traits.CoinMessagesTrait

/**
 * Generates high level RollCall Messages from protocol folder
 * For content validation: all the params are required
 * For handling: id, message with decoded data, channel are required
 */
object PostTransactionMessages extends CoinMessagesTrait {

  override val action: ActionType = ActionType.POST_TRANSACTION
  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + "coin")

  final val postTransaction: JsonRpcRequest = getJsonRPCRequestFromFile("coin/post_transaction.json")()
  final val postTransactionCoinbase: JsonRpcRequest = getJsonRPCRequestFromFile("coin/post_transaction_coinbase.json")()
  final val postTransactionBadSignature: JsonRpcRequest = getJsonRPCRequestFromFile("coin/post_transaction_bad_signature.json")()
  final val postTransactionMaxAmount: JsonRpcRequest = getJsonRPCRequestFromFile("coin/post_transaction_max_amount.json")()
  final val postTransactionOverflowAmount: JsonRpcRequest = getJsonRPCRequestFromFile("coin/post_transaction_overflow_amount.json")()
  final val postTransactionOverflowSum: JsonRpcRequest = getJsonRPCRequestFromFile("coin/post_transaction_overflow_sum.json")()
  final val postTransactionZeroAmount: JsonRpcRequest = getJsonRPCRequestFromFile("coin/post_transaction_zero_amount.json")()
  final val postTransactionNegativeAmount: JsonRpcRequest = getJsonRPCRequestFromFile("coin/post_transaction_negative_amount.json")()
  final val postTransactionWrongTransactionId: JsonRpcRequest = getJsonRPCRequestFromFile("coin/post_transaction_wrong_transaction_id.json")()
}
