package ch.epfl.pop.pubsub.graph.handlers

import akka.pattern.AskableActorRef
import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.socialMedia._
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor
import spray.json._

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object SocialMediaHandler extends MessageHandler {
  final lazy val handlerInstance = new SocialMediaHandler(super.dbActor)

  def handleAddChirp(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleAddChirp(rpcMessage)
  def handleDeleteChirp(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleDeleteChirp(rpcMessage)

  def handleNotifyAddChirp(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleNotifyAddChirp(rpcMessage)
  def handleNotifyDeleteChirp(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleNotifyDeleteChirp(rpcMessage)

  def handleAddReaction(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleAddReaction(rpcMessage)
  def handleDeleteReaction(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleDeleteReaction(rpcMessage)
}

class SocialMediaHandler(dbRef: => AskableActorRef) extends MessageHandler {

  /**
   * Overrides default DbActor with provided parameter
   */
  override final val dbActor: AskableActorRef = dbRef

  private final val unknownAnswerDatabase: String = "Database actor returned an unknown answer"

  private def generateSocialChannel(lao_id: Hash): Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + lao_id + Channel.SOCIAL_MEDIA_CHIRPS_PREFIX)


  def handleAddChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
    writeAndPropagate(rpcMessage) match {
      case Left(_) =>
        val channelChirp: Channel = rpcMessage.getParamsChannel
        channelChirp.decodeChannelLaoId match {
          case Some(lao_id) =>
            val broadcastChannel: Channel = generateSocialChannel(lao_id)
            rpcMessage.getParamsMessage match {
              case Some(params) =>
                // we can't get the message_id as a Base64Data, it is a Hash
                val chirp_id: Hash = params.message_id
                val timestamp: Timestamp = params.decodedData.get.asInstanceOf[AddChirp].timestamp
                val notifyAddChirp: NotifyAddChirp = NotifyAddChirp(chirp_id, channelChirp, timestamp)
                val broadcastData: Base64Data = Base64Data.encode(notifyAddChirp.toJson.toString)
                Await.result(dbBroadcast(rpcMessage, rpcMessage.getParamsChannel, broadcastData, broadcastChannel), duration)
              case None => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Server failed to extract chirp id for the broadcast", rpcMessage.id))
            }
          case None => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Server failed to extract LAO id for the broadcast", rpcMessage.id))
        }
      case error@Right(_) => error
      case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, unknownAnswerDatabase, rpcMessage.id))
    }
  }

  def handleDeleteChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
    writeAndPropagate(rpcMessage) match {
      case Left(_) =>
        val channelChirp: Channel = rpcMessage.getParamsChannel
        channelChirp.decodeChannelLaoId match {
          case Some(lao_id) =>
            val broadcastChannel: Channel = generateSocialChannel(lao_id)
            rpcMessage.getParamsMessage match {
              case Some(params) =>
                val chirp_id: Hash = params.message_id
                val timestamp: Timestamp = params.decodedData.get.asInstanceOf[DeleteChirp].timestamp
                val notifyDeleteChirp: NotifyDeleteChirp = NotifyDeleteChirp(chirp_id, channelChirp, timestamp)
                val broadcastData: Base64Data = Base64Data.encode(notifyDeleteChirp.toJson.toString)
                Await.result(dbBroadcast(rpcMessage, rpcMessage.getParamsChannel, broadcastData, broadcastChannel), duration)
              case None => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Server failed to extract chirp id for the broadcast", rpcMessage.id))
            }
          case None => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Server failed to extract LAO id for the broadcast", rpcMessage.id))
        }
      case error@Right(_) => error
      case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, unknownAnswerDatabase, rpcMessage.id))
    }
  }

  // no need for a case handleNotifyAddChirp or handleNotifyDeleteChirp for now, since the server never receives any in theory, but could be needed later
  def handleNotifyAddChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
    Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED: SocialMediaHandler should not handle NotifyAddChirp messages", rpcMessage.id))
  }

  def handleNotifyDeleteChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
    Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED: SocialMediaHandler should not handle NotifyDeleteChirp messages", rpcMessage.id))
  }

  def handleAddReaction(rpcMessage: JsonRpcRequest): GraphMessage = {
    writeAndPropagate(rpcMessage)
  }

  def handleDeleteReaction(rpcMessage: JsonRpcRequest): GraphMessage = {
    writeAndPropagate(rpcMessage)
  }

  private def writeAndPropagate(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }

}

