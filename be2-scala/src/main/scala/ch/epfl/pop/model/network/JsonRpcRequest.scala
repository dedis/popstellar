package ch.epfl.pop.model.network
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.{Params, ParamsWithMessage}
import ch.epfl.pop.model.network.method.message.data.MessageData
import ch.epfl.pop.model.objects.{Base64Data, Channel}
import ch.epfl.pop.pubsub.graph.PipelineError

import scala.util.{Success, Try}

class JsonRpcRequest(
                           val jsonrpc: String,
                           val method: MethodType.MethodType,
                           val params: Params,
                           val id: Option[Int]
                         ) extends JsonRpcMessage with Validatable {
  override def validateContent(): Option[PipelineError] = ??? // params.validateContent() // define recursively?s

  // defensive methods in case protocol structure changes
  def getParams: Params = this.params
  def getParamsChannel: Channel = this.params.channel
  def hasParamsMessage: Boolean = this.params.hasMessage
  def getParamsMessage: Option[Message] = Try(this.params.asInstanceOf[ParamsWithMessage].message) match {
    case Success(message) => Some(message)
    case _ => None
  }
  def getEncodedData: Option[Base64Data] = this.getParamsMessage match {
    case Some(message) => Some(message.data)
    case _ => None
  }
  def getDecodedData: Option[MessageData] = this.getParamsMessage match {
    case Some(message) => message.decodedData
    case _ => None
  }
  def setDecodedData(decodedData: MessageData): Unit = this.getParamsMessage match {
    case Some(message) => message.decodedData = Some(decodedData)
    case _ =>
  }
}

object JsonRpcRequest extends Parsable {
  def apply(
             jsonrpc: String,
             method: MethodType.MethodType,
             params: Params,
             id: Option[Int]
           ): JsonRpcRequest = {
    new JsonRpcRequest(jsonrpc, method, params, id)
  }

  override def buildFromJson(payload: String): JsonRpcRequest = ???
}
