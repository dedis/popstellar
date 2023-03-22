package ch.epfl.pop.model.network

import ch.epfl.pop.json.HighLevelProtocol._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.network.method.{Params, ParamsWithMessage}
import ch.epfl.pop.model.objects.{Base64Data, Channel, Hash}
import spray.json._

import scala.util.{Success, Try}

class JsonRpcRequest(
    val jsonrpc: String,
    val method: MethodType.MethodType,
    val params: Params,
    val id: Option[Int]
) extends JsonRpcMessage {

  // defensive methods in case protocol structure changes
  def getParams: Params = params

  def getParamsChannel: Channel = params.channel

  def hasParamsMessage: Boolean = params.hasMessage

  def getParamsMessage: Option[Message] = Try(params.asInstanceOf[ParamsWithMessage].message) match {
    case Success(message) => Some(message)
    case _                => None
  }

  def getEncodedData: Option[Base64Data] = this.getParamsMessage match {
    case Some(message) => Some(message.data)
    case _             => None
  }

  def getDecodedData: Option[MessageData] = this.getParamsMessage match {
    case Some(message) => message.decodedData
    case _             => None
  }

  def getDecodedDataHeader: (ObjectType, ActionType) = this.getParamsMessage match {
    case Some(message) => message.decodedData match {
        case Some(messageData) => (messageData._object, messageData.action)
        case _                 => (ObjectType.INVALID, ActionType.INVALID)
      }
    case _ => (ObjectType.INVALID, ActionType.INVALID)
  }

  /** @param decodedData
    *   decoded data to set to new JsonRpcRequest
    * @return
    *   a new JsonRpcRequest with decoded message data
    */
  def getWithDecodedData(decodedData: MessageData): Option[JsonRpcRequest] = this.getParamsMessage match {
    case Some(message) =>
      // Get copy of the message and sets its data to the decoded one
      val decodedMessage = message.copy(decodedData = Some(decodedData))
      // Similar to this but with new decoded message in its params
      Some(JsonRpcRequest(this.jsonrpc, this.method, new ParamsWithMessage(this.params.channel, decodedMessage), this.id))
    case None => None
  }

  def extractLaoId: Hash = this.getParamsChannel.extractChildChannel
  
  /** Returns a typed request (model/network/requests) that can be instantiated with <typedConstructor>
    *
    * @param typedConstructor
    *   a constructor able to instantiate a typed request of type <T>
    * @tparam T
    *   type of the typed request
    * @return
    *   a typed request with the same parameters as <this>
    */
  def toTypedRequest[T](typedConstructor: (String, MethodType.MethodType, Params, Option[Int]) => T): T = {
    typedConstructor(jsonrpc, method, params, id)
  }

  override def getId: Option[Int] = id
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

  override def buildFromJson(payload: String): JsonRpcRequest = payload.parseJson.asJsObject.convertTo[JsonRpcRequest]
}
