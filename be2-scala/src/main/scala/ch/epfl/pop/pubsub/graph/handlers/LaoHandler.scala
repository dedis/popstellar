package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.JsonRpcMessage
import ch.epfl.pop.model.network.requests.lao.{JsonRpcRequestCreateLao, JsonRpcRequestStateLao, JsonRpcRequestUpdateLao}

case object LaoHandler extends MessageHandler {

  override val handler: Flow[JsonRpcMessage, Nothing, NotUsed] = Flow[JsonRpcMessage].map {
    case message@(_: JsonRpcRequestCreateLao) => handleCreateLao(message); ???
    case message@(_: JsonRpcRequestStateLao) => handleStateLao(message); ???
    case message@(_: JsonRpcRequestUpdateLao) => handleUpdateLao(message); ???
    case _ => ???
  }

  def handleCreateLao(message: JsonRpcMessage) {}
  def handleStateLao(message: JsonRpcMessage) {}
  def handleUpdateLao(message: JsonRpcMessage) {}
}
