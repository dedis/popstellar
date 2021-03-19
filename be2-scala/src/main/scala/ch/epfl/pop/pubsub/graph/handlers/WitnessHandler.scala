package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.JsonRpcMessage
import ch.epfl.pop.model.network.requests.witness.JsonRpcRequestWitnessMessage

case object WitnessHandler extends MessageHandler {

  override val handler: Flow[JsonRpcMessage, Nothing, NotUsed] = Flow[JsonRpcMessage].map {
    case message@(_: JsonRpcRequestWitnessMessage) => handleWitnessMessage(message); ???
    case _ => ???
  }

  def handleWitnessMessage(message: JsonRpcMessage) {}
}
