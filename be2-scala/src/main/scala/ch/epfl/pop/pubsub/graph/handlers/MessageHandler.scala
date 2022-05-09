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
   * @param rpcRequest request containing the message
   * @return the database answer wrapped in a [[scala.concurrent.Future]]
   */
  def dbAskWrite(rpcRequest: JsonRpcRequest): Future[GraphMessage] = {
    val m: Message = rpcRequest.getParamsMessage.getOrElse(
      return Future {
        Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"dbAskWrite failed : retrieve empty rpcRequest message", rpcRequest.id))
      }
    )

    val askWrite = dbActor ? DbActor.Write(rpcRequest.getParamsChannel, m)
    askWrite.transformWith {
      case Success(_) => Future(Left(rpcRequest))
      case _ => Future(Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"dbAskWrite failed : could not write message $m", rpcRequest.id)))
    }
  }

  /**
   * Asks the database to store the message contained in <rpcMessage> (or the provided message) as well as
   * propagate its content to clients subscribed to the rpcMessage's channel
   *
   * @param rpcRequest request containing the message
   * @return the database answer wrapped in a [[scala.concurrent.Future]]
   */
  def dbAskWritePropagate(rpcRequest: JsonRpcRequest): Future[GraphMessage] = {
    val m: Message = rpcRequest.getParamsMessage.getOrElse(
      return Future {
        Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"dbAskWritePropagate failed : retrieve empty rpcRequest message", rpcRequest.id))
      }
    )

    val askWritePropagate = dbActor ? DbActor.WriteAndPropagate(rpcRequest.getParamsChannel, m)
    askWritePropagate.transformWith {
      case Success(_) => Future(Left(rpcRequest))
      case _ => Future(Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"dbAskWritePropagate failed : could not write & propagate message $m", rpcRequest.id)))
    }
  }
}
