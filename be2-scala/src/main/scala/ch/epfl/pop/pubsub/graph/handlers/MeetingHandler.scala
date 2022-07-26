package ch.epfl.pop.pubsub.graph.handlers

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}

import scala.concurrent.{Await, Future}

case object MeetingHandler extends MessageHandler {

  def handleCreateMeeting(rpcMessage: JsonRpcRequest): GraphMessage = {
    rpcMessage.getParamsChannel.decodeChannelLaoId match {
      case Some(_) =>
        val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
        Await.result(ask, duration)
      case _ => Right(PipelineError(
          ErrorCodes.INVALID_DATA.id,
          s"Unable to create meeting: invalid encoded laoId '${rpcMessage.getParamsChannel}'",
          rpcMessage.id
        ))
    }
  }

  def handleStateMeeting(rpcMessage: JsonRpcRequest): GraphMessage = {
    Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED : handleStateMeeting is not implemented", rpcMessage.id))
  }
}
