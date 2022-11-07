package ch.epfl.pop.pubsub.graph.handlers

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.DbActorNAckException
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

case object CoinHandler extends MessageHandler {

  def handlePostTransaction(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask =
      for {
        _ <- checkParameters(rpcMessage, s"Unable to handle coin message $rpcMessage. Not a post message")
        message: Message = rpcMessage.getParamsMessage.get
        _ <- dbActor ? DbActor.WriteAndPropagate(rpcMessage.getParamsChannel, message)
      } yield ()

      Await.ready(ask, duration).value match {
        case Some(Success(_))                        => Left(rpcMessage)
        case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"handlePostTransaction failed : ${ex.message}", rpcMessage.getId))
        case reply                                   => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handlePostTransaction failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
      }
  }
}
