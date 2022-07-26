package ch.epfl.pop.model.network

import ch.epfl.pop.json.HighLevelProtocol._
import spray.json._

final case class JsonRpcResponse(
    jsonrpc: String,
    result: Option[ResultObject],
    error: Option[ErrorObject],
    id: Option[Int]
) extends JsonRpcMessage {
  def isPositive: Boolean = result.isDefined
  override def getId: Option[Int] = id
}

object JsonRpcResponse extends Parsable {
  def apply(
      jsonrpc: String,
      result: ResultObject,
      id: Option[Int]
  ): JsonRpcResponse = {
    new JsonRpcResponse(jsonrpc, Some(result), None, id)
  }

  def apply(
      jsonrpc: String,
      error: ErrorObject,
      id: Option[Int]
  ): JsonRpcResponse = {
    new JsonRpcResponse(jsonrpc, None, Some(error), id)
  }

  override def buildFromJson(payload: String): JsonRpcResponse = payload.parseJson.asJsObject.convertTo[JsonRpcResponse]
}
