package ch.epfl.pop.pubsub.graph.handlers

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.objects.DbActorNAckException
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await}
import scala.util.{Failure, Success}

case object CoinHandler extends MessageHandler {

  def handlePostTransaction(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask = {
      for {
        _ <- extractParameters(rpcMessage, s"Unable to handle coin message $rpcMessage. Not a post message")
        _ <- dbAskWritePropagate(rpcMessage)
      } yield ()
    }

    Await.ready(ask, duration).value match {
      case Some(Success(_))                        => Left(rpcMessage)
      case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"handlePostTransaction failed : ${ex.message}", rpcMessage.getId))
      case reply                                   => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handlePostTransaction failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
    }
  }
}
