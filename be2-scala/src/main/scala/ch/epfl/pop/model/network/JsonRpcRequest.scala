package ch.epfl.pop.model.network
import ch.epfl.pop.model.network.method.Params
import ch.epfl.pop.model.network.method.message.data.MessageData

case class JsonRpcRequest(
                           jsonrpc: String,
                           method: Method.Method,
                           params: Params,
                           id: Option[Int]
                         ) extends JsonRpcMessage

object JsonRpcRequest extends Parsable {
  def apply(
             jsonrpc: String,
             method: Method.Method,
             params: Params,
             id: Option[Int]
           ): JsonRpcRequest = {
    // FIXME add checks
    new JsonRpcRequest(jsonrpc, method, params, id)
  }

  override def buildFromJson(messageData: MessageData, payload: String): JsonRpcRequest = ???
}
