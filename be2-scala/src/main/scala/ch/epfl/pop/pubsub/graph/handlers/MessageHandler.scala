package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.JsonRpcMessage

trait MessageHandler {
  val handler: Flow[JsonRpcMessage, Nothing, NotUsed] // FIXME Nothing. What will it be?
}
