package ch.epfl.pop.pubsub.graph.handlers

import akka.pattern.AskableActorRef
import ch.epfl.pop.json.HighLevelProtocol.messageFormat
import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.socialMedia._
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object SocialMediaHandler extends MessageHandler {
  private final val unknownAnswerDatabase: String = "Database actor returned an unknown answer"

  private def generateSocialChannel(laoId: Hash): Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + laoId + Channel.SOCIAL_MEDIA_CHIRPS_PREFIX)

  def handleAddChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask =
      for {
        _ <- extractLaoChannel(rpcMessage, "Server failed to extract LAO id for the broadcast")
        _ <- extractParameters[AddChirp](rpcMessage, "Server failed to extract chirp id for the broadcast")
        _ <- dbActor ? DbActor.WriteAndPropagate(rpcMessage.getParamsChannel, rpcMessage.getParamsMessage.get)
      } yield ()

    Await.ready(ask, duration).value match {
      case Some(Success(_)) =>
        val (chirpId, channelChirp, data, broadcastChannel) = parametersToBroadcast[AddChirp](rpcMessage)
        //  create and propagate the notifyAddChirp message
        val notifyAddChirp: NotifyAddChirp = NotifyAddChirp(chirpId, channelChirp, data.timestamp)
        dbActor ? DbActor.WriteAndPropagate(broadcastChannel, rpcMessage.getParamsMessage.get)
        Await.result(
          broadcast(rpcMessage, channelChirp, notifyAddChirp.toJson, broadcastChannel, writeToDb = false),
          duration
        )

      case Some(Failure(ex: DbActorNAckException)) => Left(PipelineError(
          ex.code,
          s"handleAddChirp failed : ${ex.message}",
          rpcMessage.getId
        ))

      case _ => Left(PipelineError(
          ErrorCodes.SERVER_ERROR.id,
          unknownAnswerDatabase,
          rpcMessage.getId
        ))
    }
  }

  def handleDeleteChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask =
      for {
        _ <- extractLaoChannel(rpcMessage, "Server failed to extract LAO id for the broadcast")
        _ <- extractParameters(rpcMessage, "Server failed to extract chirp id for the broadcast")
        _ <- dbActor ? DbActor.WriteAndPropagate(rpcMessage.getParamsChannel, rpcMessage.getParamsMessage.get)
      } yield ()

    Await.ready(ask, duration).value match {
      case Some(Success(_)) =>
        val (chirpId, channelChirp, data, broadcastChannel) = parametersToBroadcast[DeleteChirp](rpcMessage)
        // create and propagate the notifyDeleteChirp message
        val notifyDeleteChirp: NotifyDeleteChirp = NotifyDeleteChirp(chirpId, channelChirp, data.timestamp)
        dbActor ? DbActor.WriteAndPropagate(broadcastChannel, rpcMessage.getParamsMessage.get)
        Await.result(
          broadcast(rpcMessage, channelChirp, notifyDeleteChirp.toJson, broadcastChannel, writeToDb = false),
          duration
        )

      case Some(Failure(ex: DbActorNAckException)) => Left(PipelineError(
          ex.code,
          s"handleDeleteChirp failed : ${ex.message}",
          rpcMessage.getId
        ))

      case _ => Left(PipelineError(
          ErrorCodes.SERVER_ERROR.id,
          unknownAnswerDatabase,
          rpcMessage.getId
        ))
    }
  }

  def handleNotifyAddChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }

  def handleNotifyDeleteChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }

  def handleAddReaction(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    dbActor ? DbActor.UpdateNumberOfNewChirpsReactions(rpcMessage.getParamsChannel)
    Await.result(ask, duration)
  }

  def handleDeleteReaction(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    dbActor ? DbActor.UpdateNumberOfNewChirpsReactions(rpcMessage.getParamsChannel)
    Await.result(ask, duration)
  }

  /** Helper function that extracts the useful parameters from the message
    *
    * @param rpcMessage
    *   : message from which we extract the parameters
    * @return
    *   the id of the chirp, the channel, the decoded data and the channel in which we broadcast
    */
  private def parametersToBroadcast[T](rpcMessage: JsonRpcRequest): (Hash, Channel, T, Channel) = {
    val channelChirp: Channel = rpcMessage.getParamsChannel
    val laoId: Hash = channelChirp.decodeChannelLaoId.get
    val broadcastChannel: Channel = generateSocialChannel(laoId)
    val params: Message = rpcMessage.getParamsMessage.get
    val chirpId: Hash = params.message_id
    val data: T = params.decodedData.get.asInstanceOf[T]

    (chirpId, channelChirp, data, broadcastChannel)
  }
}
