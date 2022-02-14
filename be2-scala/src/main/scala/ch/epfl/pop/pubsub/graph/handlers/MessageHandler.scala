package ch.epfl.pop.pubsub.graph.handlers

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.DbActorNAckException
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.{DbActor, ErrorCodes, GraphMessage, PipelineError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure

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
    val ask: Future[GraphMessage] = (dbActor ? DbActor.Write(rpcMessage.getParamsChannel, m)).map {
      case DbActor.DbActorWriteAck() => Left(rpcMessage)
      case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, unknownAnswer, rpcMessage.id))
    }.recover{
      case e: DbActorNAckException => Right(PipelineError(e.getCode, e.getMessage, rpcMessage.id))
      case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, unknownAnswer, rpcMessage.id))
    }

    ask
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
    val ask: Future[GraphMessage] = (dbActor ? DbActor.WriteAndPropagate(rpcMessage.getParamsChannel, m)).map {
      case DbActor.DbActorWriteAck() => Left(rpcMessage)
      case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, unknownAnswer, rpcMessage.id))
    }.recover{
      case e: DbActorNAckException => Right(PipelineError(e.getCode, e.getMessage, rpcMessage.id))
      case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, unknownAnswer, rpcMessage.id))
    }

    ask
  }
}
