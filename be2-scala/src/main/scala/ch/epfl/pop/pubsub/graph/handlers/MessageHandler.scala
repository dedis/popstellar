package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.pubsub.graph.{DbActorNew, ErrorCodes, GraphMessage, PipelineError}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.util.Success

import scala.language.postfixOps

trait MessageHandler {
  val askMaxTimeout: FiniteDuration = 1000 millis

  val handler: Flow[GraphMessage, GraphMessage, NotUsed]

  def dbAskWritePropagate(rpcMessage: JsonRpcRequest): GraphMessage = {
    val paramsMessage: Option[Message] = rpcMessage.getParamsMessage
    paramsMessage match {
      case Some(message) => dbActor.ask(ref => DbActorNew.Write(rpcMessage.getParamsChannel, message, ref)) match {
        case Success(_) =>
          // FIXME propagate
          Left(rpcMessage)
        case _ => Right(PipelineError(-10, "")) // FIXME add DbActor "answers" with error description if failed
      }
      case _ => Right(PipelineError(ErrorCodes.INVALID_DATA.id, s"RPC-params does not contain any message"))
    }
  }
}
