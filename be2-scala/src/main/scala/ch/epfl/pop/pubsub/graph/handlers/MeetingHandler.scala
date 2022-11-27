package ch.epfl.pop.pubsub.graph.handlers

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.objects.DbActorNAckException
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await}
import scala.util.{Failure, Success}

case object MeetingHandler extends MessageHandler {

  def handleCreateMeeting(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask = {
      for {
        _ <- checkParameters(rpcMessage, s"Unable to handle coin message $rpcMessage. Not a post message")
        _ <- dbAskWritePropagate(rpcMessage)
      } yield ()
    }

    Await.ready(ask, duration).value match {
      case Some(Success(_))                        => Left(rpcMessage)
      case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"Unable to create meeting: invalid encoded laoId '${rpcMessage.getParamsChannel}'", rpcMessage.getId))
      case reply                                   => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleCreateMeeting failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
    }
  }

  def handleStateMeeting(rpcMessage: JsonRpcRequest): GraphMessage = {
    Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED : handleStateMeeting is not implemented", rpcMessage.id))
  }
}
