package ch.epfl.pop.model.network
import ch.epfl.pop.json.JsonUtils.JsonMessageParserError
import ch.epfl.pop.model.network.method.Params
import ch.epfl.pop.model.network.method.message.data.MessageData
import ch.epfl.pop.pubsub.graph.PipelineError

class JsonRpcRequest(
                           val jsonrpc: String,
                           val method: MethodType.MethodType,
                           val params: Params,
                           val id: Option[Int]
                         ) extends JsonRpcMessage with Validatable {
  override def validateContent(): Option[PipelineError] = ??? // params.validateContent() // define recursively?s
}

object JsonRpcRequest extends Parsable {
  def apply(
             jsonrpc: String,
             method: MethodType.MethodType,
             params: Params,
             id: Option[Int]
           ): JsonRpcRequest = {
    // FIXME add checks
    new JsonRpcRequest(jsonrpc, method, params, id)
  }

  override def buildFromJson(messageData: MessageData, payload: String): JsonRpcRequest = ???
}
