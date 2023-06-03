package ch.epfl.pop.pubsub.graph.handlers

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.pubsub.graph.GraphMessage
import ch.epfl.pop.storage.DbActor

/** Handler for Popcha related messages
  */
object PopchaHandler extends MessageHandler {

  /** Handles authentication messages
    * @param rpcMessage
    *   message received
    * @return
    *   a graph message representing the message's state after handling
    */
  def handleAuthentication(rpcMessage: JsonRpcRequest): GraphMessage = {
    // TODO: add actual handling
    Right(rpcMessage)
  }
}
