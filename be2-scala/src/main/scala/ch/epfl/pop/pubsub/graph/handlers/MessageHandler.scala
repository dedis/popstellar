package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.DbActor.{DbActorNAck, DbActorWriteAck}
import ch.epfl.pop.pubsub.graph.{DbActor, ErrorCodes, GraphMessage, PipelineError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

trait MessageHandler extends AskPatternConstants {
  implicit final val dbActor: AskableActorRef = DbActor.getInstance

  val handler: Flow[GraphMessage, GraphMessage, NotUsed]

  def dbAskWritePropagate(rpcMessage: JsonRpcRequest): GraphMessage = {
    val paramsMessage: Option[Message] = rpcMessage.getParamsMessage
    paramsMessage match {
      case Some(message: Message) =>
        val f: Future[GraphMessage] = (dbActor ? DbActor.Write(rpcMessage.getParamsChannel, message)).map {
          case DbActorWriteAck =>
            // FIXME propagate useful this function?
            println("++ Propaaagaaaating in message handler")
            Left(rpcMessage)
          case DbActorNAck(code, description) => Right(PipelineError(code, description, rpcMessage.id))
          case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Database actor returned an unknown answer", rpcMessage.id))
        }

        Await.result(f, duration)

      case _ => Right(PipelineError(ErrorCodes.INVALID_DATA.id, s"RPC-params does not contain any message", rpcMessage.id))
    }
  }
}
