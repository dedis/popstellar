package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.JsonRpcMessage
import ch.epfl.pop.model.network.requests.rollCall.{JsonRpcRequestCloseRollCall, JsonRpcRequestCreateRollCall, JsonRpcRequestOpenRollCall, JsonRpcRequestReopenRollCall}

case object RollCallHandler extends MessageHandler {

  override val handler: Flow[JsonRpcMessage, Nothing, NotUsed] = Flow[JsonRpcMessage].map {
    case message@(_: JsonRpcRequestCreateRollCall) => handleCreateRollCall(message); ???
    case message@(_: JsonRpcRequestOpenRollCall) => handleOpenRollCall(message); ???
    case message@(_: JsonRpcRequestReopenRollCall) => handleReopenRollCall(message); ???
    case message@(_: JsonRpcRequestCloseRollCall) => handleCloseRollCall(message); ???
    case _ => ???
  }

  def handleCreateRollCall(message: JsonRpcMessage) {}
  def handleOpenRollCall(message: JsonRpcMessage) {}
  def handleReopenRollCall(message: JsonRpcMessage) {}
  def handleCloseRollCall(message: JsonRpcMessage) {}
}
