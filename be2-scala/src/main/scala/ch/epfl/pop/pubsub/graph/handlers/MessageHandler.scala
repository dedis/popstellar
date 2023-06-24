package ch.epfl.pop.pubsub.graph.handlers

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.{Base64Data, Channel, Hash, Signature}
import ch.epfl.pop.pubsub.{AskPatternConstants, PubSubMediator, PublishSubscribe}
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.DbActorReadLaoDataAck
import spray.json.JsValue

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

trait MessageHandler extends AskPatternConstants {

  /** May be overridden by the reference of the used DbActor
    */
  def dbActor: AskableActorRef = PublishSubscribe.getDbActorRef
  def connectionMediator: AskableActorRef = PublishSubscribe.getConnectionMediatorRef

  def mediator: AskableActorRef = PublishSubscribe.getMediatorActorRef

  def extractParameters[T](rpcRequest: JsonRpcRequest, errorMsg: String): Future[(GraphMessage, Message, Option[T])] = {
    rpcRequest.getParamsMessage match {
      case Some(_) =>
        val message: Message = rpcRequest.getParamsMessage.get
        val data: T = message.decodedData.get.asInstanceOf[T]
        Future((Right(rpcRequest), message, Some(data)))
      case _ => Future((Left(PipelineError(ErrorCodes.SERVER_ERROR.id, errorMsg, rpcRequest.id)), null, None))
    }
  }

  def extractLaoChannel(rpcRequest: JsonRpcRequest, errorMsg: String): Future[(GraphMessage, Option[Hash])] = {
    rpcRequest.getParamsChannel.decodeChannelLaoId match {
      case optId @ Some(_) => Future((Right(rpcRequest), optId))
      case _               => Future((Left(PipelineError(ErrorCodes.SERVER_ERROR.id, errorMsg, rpcRequest.id)), None))
    }
  }

  /** Asks the database to store the message contained in <rpcMessage> (or the provided message)
    *
    * @param rpcRequest
    *   request containing the message
    * @return
    *   the database answer wrapped in a [[scala.concurrent.Future]]
    */
  def dbAskWrite(rpcRequest: JsonRpcRequest): Future[GraphMessage] = {
    val m: Message = rpcRequest.getParamsMessage.getOrElse(
      return Future {
        Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"dbAskWrite failed : retrieve empty rpcRequest message", rpcRequest.id))
      }
    )

    val askWrite = dbActor ? DbActor.Write(rpcRequest.getParamsChannel, m)
    askWrite.transformWith {
      case Success(_) => Future(Right(rpcRequest))
      case _          => Future(Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"dbAskWrite failed : could not write message $m", rpcRequest.id)))
    }
  }

  /** Asks the database to store the message contained in <rpcMessage> (or the provided message) as well as propagate its content to clients subscribed to the rpcMessage's channel
    *
    * @param rpcRequest
    *   request containing the message
    * @return
    *   the database answer wrapped in a [[scala.concurrent.Future]]
    */
  def dbAskWritePropagate(rpcRequest: JsonRpcRequest): Future[GraphMessage] = {
    val m: Message = rpcRequest.getParamsMessage.getOrElse(
      return Future {
        Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"dbAskWritePropagate failed : retrieve empty rpcRequest message", rpcRequest.id))
      }
    )

    val askWritePropagate = dbActor ? DbActor.WriteAndPropagate(rpcRequest.getParamsChannel, m)
    askWritePropagate.transformWith {
      case Success(_) => Future(Right(rpcRequest))
      case _          => Future(Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"dbAskWritePropagate failed : could not write & propagate message $m", rpcRequest.id)))
    }
  }

  /** Helper function for broadcasting messages to subscribers
    *
    * @param rpcMessage
    *   : message for which we want to generate the broadcast
    * @param channel
    *   : the Channel in which we read the data
    * @param broadcastData
    *   : the message data we broadcast converted to Base64Data
    * @param broadcastChannel
    *   : the Channel in which we broadcast
    * @param writeToDb
    *   : write to db when true, only broadcast when set to false.
    * @return
    *   the database answer wrapped in a [[scala.concurrent.Future]]
    */
  def broadcast(rpcMessage: JsonRpcRequest, channel: Channel, broadcastData: JsValue, broadcastChannel: Channel, writeToDb: Boolean = true): Future[GraphMessage] = {
    val m: Message = rpcMessage.getParamsMessage.getOrElse(
      return Future {
        Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"dbAskWritePropagate failed : retrieve empty rpcRequest message", rpcMessage.id))
      }
    )

    val combined = for {
      DbActorReadLaoDataAck(laoData) <- dbActor ? DbActor.ReadLaoData(channel)
      encodedData: Base64Data = Base64Data.encode(broadcastData.toString)
      broadcastSignature: Signature = laoData.privateKey.signData(encodedData)
      broadcastId: Hash = Hash.fromStrings(encodedData.toString, broadcastSignature.toString)
      broadcastMessage: Message = Message(encodedData, laoData.publicKey, broadcastSignature, broadcastId, List.empty)
      _ <-
        if (writeToDb) {
          dbActor ? DbActor.WriteAndPropagate(broadcastChannel, broadcastMessage)
        } else {
          mediator ? PubSubMediator.Propagate(broadcastChannel, broadcastMessage)
        }
    } yield ()

    combined.transformWith {
      case Success(_) => Future(Right(rpcMessage))
      case _          => Future(Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"broadcast failed : could not read and broadcast message $m", rpcMessage.id)))
    }
  }
}
