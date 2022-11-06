package ch.epfl.pop.pubsub.graph.handlers

import akka.pattern.AskableActorRef
import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.socialMedia._
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
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

  /** Overrides default DbActor with provided parameter
    */
  override final val dbActor: AskableActorRef = dbRef

  private final val unknownAnswerDatabase: String = "Database actor returned an unknown answer"

  private def generateSocialChannel(lao_id: Hash): Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + lao_id + Channel.SOCIAL_MEDIA_CHIRPS_PREFIX)

  def handleAddChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask =
      for {
        _ <- dbAskWritePropagate(rpcMessage)
        _ <- checkLaoChannel(rpcMessage, "Server failed to extract LAO id for the broadcast")
        _ <- checkParameters(rpcMessage, "Server failed to extract chirp id for the broadcast")
      } yield ()

    Await.ready(ask, duration).value match {
      case Some(Success(_)) =>
        val channelChirp: Channel = rpcMessage.getParamsChannel
        val lao_id: Hash = channelChirp.decodeChannelLaoId.get
        val broadcastChannel: Channel = generateSocialChannel(lao_id)

        val params: Message = rpcMessage.getParamsMessage.get
        val chirp_id: Hash = params.message_id
        val timestamp: Timestamp = params.decodedData.get.asInstanceOf[AddChirp].timestamp
        val notifyAddChirp: NotifyAddChirp = NotifyAddChirp(chirp_id, channelChirp, timestamp)
        val broadcastData: Base64Data = Base64Data.encode(notifyAddChirp.toJson.toString)

        Await.result(dbBroadcast(rpcMessage, rpcMessage.getParamsChannel, broadcastData, broadcastChannel), duration)

      case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"handleAddChirp failed : ${ex.message}", rpcMessage.getId))
      case _                                       => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, unknownAnswerDatabase, rpcMessage.getId))
    }
  }

  def handleDeleteChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask =
      for {
        _ <- dbAskWritePropagate(rpcMessage)
        _ <- checkLaoChannel(rpcMessage, "Server failed to extract LAO id for the broadcast")
        _ <- checkParameters(rpcMessage, "Server failed to extract chirp id for the broadcast")
      } yield ()

    Await.ready(ask, duration).value match {
      case Some(Success(_)) =>
        val channelChirp: Channel = rpcMessage.getParamsChannel
        val lao_id: Hash = channelChirp.decodeChannelLaoId.get
        val broadcastChannel: Channel = generateSocialChannel(lao_id)

        val params: Message = rpcMessage.getParamsMessage.get
        val chirp_id: Hash = params.message_id
        val timestamp: Timestamp = params.decodedData.get.asInstanceOf[DeleteChirp].timestamp
        val notifyDeleteChirp: NotifyDeleteChirp = NotifyDeleteChirp(chirp_id, channelChirp, timestamp)
        val broadcastData: Base64Data = Base64Data.encode(notifyDeleteChirp.toJson.toString)

        Await.result(dbBroadcast(rpcMessage, rpcMessage.getParamsChannel, broadcastData, broadcastChannel), duration)

      case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"handleDeleteChirp failed : ${ex.message}", rpcMessage.getId))
      case _                                       => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, unknownAnswerDatabase, rpcMessage.getId))
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

}
