package ch.epfl.pop.pubsub.graph.handlers

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

trait MessageHandler extends AskPatternConstants {

  /**
   * May be overridden by the reference of the used DbActor
   */
  def dbActor: AskableActorRef = DbActor.getInstance

  /**
   * Asks the database to store the message contained in <rpcMessage> (or the provided message)
   *
   * @param rpcMessage request containing the message
   * @return the database answer wrapped in a [[scala.concurrent.Future]]
   */
  def dbAskWrite(rpcMessage: JsonRpcRequest, message: Message = null): Future[GraphMessage] = {
    val m: Message = if (message != null) message else rpcMessage.getParamsMessage.get

    val askWrite = dbActor ? DbActor.Write(rpcMessage.getParamsChannel, m)
    askWrite.transformWith {
      case Success(_) => Future(Left(rpcMessage))
      case _ => Future(Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"dbAskWrite failed : could not write message $message", rpcMessage.id)))
    }
  }

  /**
   * Asks the database to store the message contained in <rpcMessage> (or the provided message) as well as
   * propagate its content to clients subscribed to the rpcMessage's channel
   *
   * @param rpcMessage request containing the message
   * @param message    (optional) message to store
   * @return the database answer wrapped in a [[scala.concurrent.Future]]
   */
  def dbAskWritePropagate(rpcMessage: JsonRpcRequest, message: Message = null): Future[GraphMessage] = {
    val m: Message = if (message != null) message else rpcMessage.getParamsMessage.get

    val askWritePropagate = dbActor ? DbActor.WriteAndPropagate(rpcMessage.getParamsChannel, m)
    askWritePropagate.transformWith {
      case Success(_) => Future(Left(rpcMessage))
      case _ => Future(Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"dbAskWritePropagate failed : could not write & propagate message $message", rpcMessage.id)))
    }
  }
}
