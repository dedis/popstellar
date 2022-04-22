package ch.epfl.pop.pubsub.graph.handlers

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}

import scala.concurrent.{Await, Future}

case object MeetingHandler extends MessageHandler {

  def handleCreateMeeting(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }

  def handleStateMeeting(rpcMessage: JsonRpcRequest): GraphMessage = {
    Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED : handleStateMeeting is not implemented", rpcMessage.id))
  }
}
