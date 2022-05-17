package util.examples.data

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.ActionType
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.objects.Channel
import util.examples.data.traits.CashMessagesTrait

/**
 * Generates high level RollCall Messages from protocol folder
 * For content validation: all the params are required
 * For handling: id, message with decoded data, channel are required
 */
object PostTransactionMessages extends CashMessagesTrait {

  override val action: ActionType = ActionType.POST
  override val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + "coin")

  final val postTransaction: JsonRpcRequest = getJsonRPCRequestFromFile("cash/post_transaction.json")()
  final val postTransactionMaxAmount: JsonRpcRequest = getJsonRPCRequestFromFile("cash/post_transaction_max_amount.json")()
  final val postTransactionOverflowAmount: JsonRpcRequest = getJsonRPCRequestFromFile("cash/post_transaction_overflow_amount.json")()
  final val postTransactionZeroAmount: JsonRpcRequest = getJsonRPCRequestFromFile("cash/post_transaction_zero_amount.json")()
  final val postTransactionNegativeAmount: JsonRpcRequest = getJsonRPCRequestFromFile("cash/post_transaction_negative_amount.json")()
  final val postTransactionWrongTransactionId: JsonRpcRequest = getJsonRPCRequestFromFile("cash/post_transaction_wrong_transaction_id.json")()
}
