package ch.epfl.pop.pubsub.graph.handlers

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.{Base64Data, Channel, DbActorNAckException, Hash, Signature}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

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

    val askWrite = dbActor ? DbActor.Write(rpcRequest.getParamsChannel)
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

  /**
   * Helper function for broadcasting messages to subscribers
   *
   * @param rpcMessage       : message for which we want to generate the broadcast
   * @param channel          : the Channel in which we read the data
   * @param broadcastData    : the message data we broadcast converted to Base64Data
   * @param broadcastChannel : the Channel in which we broadcast
   */
  def dbBroadcast(rpcMessage: JsonRpcRequest, channel: Channel, broadcastData: Base64Data, broadcastChannel: Channel): GraphMessage = {
    val askLaoData = dbActor ? DbActor.ReadLaoData(channel)

    Await.ready(askLaoData, duration).value match {
      case Some(Success(DbActor.DbActorReadLaoDataAck(laoData))) =>
        val broadcastSignature: Signature = laoData.keyPair.privateKey.signData(broadcastData)
        val broadcastId: Hash = Hash.fromStrings(broadcastData.toString, broadcastSignature.toString)
        val broadcastMessage: Message = Message(broadcastData, laoData.keyPair.publicKey, broadcastSignature, broadcastId, List.empty)

        val askWritePropagate = dbActor ? DbActor.WriteAndPropagate(broadcastChannel, broadcastMessage)
        Await.ready(askWritePropagate, duration).value.get match {
          case Success(_) => Left(rpcMessage)
          case Failure(ex: DbActorNAckException) => Right(PipelineError(ex.code, s"broadcastHelper failed : ${ex.message}", rpcMessage.getId))
          case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"broadcastHelper failed : unknown DbActor reply $reply", rpcMessage.getId))
        }

      case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"broadcastHelper failed : ${ex.message}", rpcMessage.getId))
      case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"broadcastHelper failed : unknown DbActor reply $reply", rpcMessage.getId))
    }
  }
}
