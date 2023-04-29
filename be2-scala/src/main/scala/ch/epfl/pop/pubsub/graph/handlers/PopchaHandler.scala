package ch.epfl.pop.pubsub.graph.handlers

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.GraphMessage

object PopchaHandler extends MessageHandler {
  def handleAuthentication(rpcMessage: JsonRpcRequest): GraphMessage = {
    Right(rpcMessage)
  }
}
