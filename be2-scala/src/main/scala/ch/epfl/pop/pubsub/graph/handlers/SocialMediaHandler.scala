package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.socialMedia._
import ch.epfl.pop.model.network.requests.socialMedia._
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.model.objects.{Channel, Hash, Base64Data, Signature, Timestamp, PublicKey}
import ch.epfl.pop.pubsub.graph.{DbActor, ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.pubsub.graph.validators.SocialMediaValidator
import ch.epfl.pop.json.MessageDataProtocol._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

import spray.json._

case object SocialMediaHandler extends MessageHandler {

    override val handler: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Left(jsonRpcMessage) => jsonRpcMessage match {
      case message@(_: JsonRpcRequestAddChirp) => handleAddChirp(message)
      case message@(_: JsonRpcRequestDeleteChirp) => handleDeleteChirp(message)
      case message@(_: JsonRpcRequestNotifyAddChirp) => handleAddChirp(message)
      case message@(_: JsonRpcRequestNotifyDeleteChirp) => handleDeleteChirp(message)
      case _ => Right(PipelineError(
        ErrorCodes.SERVER_ERROR.id,
        "Internal server fault: SocialMediaHandler was given a message it could not recognize",
        jsonRpcMessage match {
          case r: JsonRpcRequest => r.id
          case r: JsonRpcResponse => r.id
          case _ => None
        }
      ))
    }
    case graphMessage@_ => graphMessage
  }

  private final val unknownAnswerDatabase: String = "Database actor returned an unknown answer"

  /**
   * Helper function for both Social Media broadcasts
   * @param rpcMessage : message for which we want to generate the broadcast
   * @param broadcastData : the message data we broadcast converted to Base64Data
   * @param broadcastChannel : the Channel in which we broadcast 
   */
  private def broadcastHelper(rpcMessage: JsonRpcRequest, broadcastData: Base64Data, broadcastChannel: Channel): GraphMessage = {
    val askLaoData = (dbActor ? DbActor.ReadLaoData(rpcMessage.getParamsChannel))
    Await.result(askLaoData, duration) match {
      case DbActor.DbActorReadLaoDataAck(Some(laoData)) => {
        val broadcastSignature: Signature = laoData.privateKey.signData(broadcastData)
        val broadcastId: Hash = Hash.fromStrings(broadcastData.toString, broadcastSignature.toString)
        //FIXME: once consensus is implemented, fix the WitnessSignaturePair list handling
        val broadcastMessage: Message = Message(broadcastData, laoData.publicKey, broadcastSignature, broadcastId, List.empty)
        val ask: Future[GraphMessage] = (dbActor ? DbActor.WriteAndPropagate(broadcastChannel, broadcastMessage)).map {
          case DbActor.DbActorWriteAck() => Left(rpcMessage)
          case DbActor.DbActorNAck(code, description) => Right(PipelineError(code, description, rpcMessage.id))
          case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, unknownAnswerDatabase, rpcMessage.id))
        }
        Await.result(ask, duration)
      }
      case DbActor.DbActorReadLaoDataAck(None) => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Can't fetch LaoData", rpcMessage.id))
      case DbActor.DbActorNAck(code, description) => Right(PipelineError(code, description, rpcMessage.id))
    }
  }
  
  def handleAddChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration) match {
      case Left(msg) => {
        val channelChirp: Channel = rpcMessage.getParamsChannel
        channelChirp.decodeSubChannel match {
          case(Some(lao_id)) => {
            val broadcastChannel: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode(lao_id) + Channel.SOCIAL_MEDIA_POSTS_PREFIX)
            rpcMessage.getParamsMessage match {
              case Some(params) => {
                // we can't get the message_id as a Base64Data, it is a Hash
                val chirp_id: Hash = params.message_id
                val timestamp: Timestamp = params.decodedData.get.asInstanceOf[AddChirp].timestamp
                val notifyAddChirp: NotifyAddChirp = NotifyAddChirp(chirp_id, channelChirp, timestamp)
                val broadcastData: Base64Data = Base64Data.encode(notifyAddChirp.toJson.toString)
                
                broadcastHelper(rpcMessage, broadcastData, broadcastChannel)
              }
              case None => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Server failed to extract chirp id for the broadcast", rpcMessage.id))
            }
          }
          case None => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Server failed to extract LAO id for the broadcast", rpcMessage.id))
        }
      }
      case error@Right(_) =>
        error
      case _ =>
        Right(PipelineError(ErrorCodes.SERVER_ERROR.id, unknownAnswerDatabase, rpcMessage.id))
    }  
  }

  def handleDeleteChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration) match {
      case Left(msg) => {
        val channelChirp: Channel = rpcMessage.getParamsChannel
        channelChirp.decodeSubChannel match {
          case(Some(lao_id)) => {
            val broadcastChannel: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode(lao_id) + Channel.SOCIAL_MEDIA_POSTS_PREFIX)
            rpcMessage.getParamsMessage match {
              case Some(params) => {
                // we can't get the message_id as a Base64Data, it is a Hash
                val chirp_id: Hash = params.message_id
                val timestamp: Timestamp = params.decodedData.get.asInstanceOf[DeleteChirp].timestamp
                val notifyDeleteChirp: NotifyDeleteChirp = NotifyDeleteChirp(chirp_id, channelChirp, timestamp)
                val broadcastData: Base64Data = Base64Data.encode(notifyDeleteChirp.toJson.toString)
                
                broadcastHelper(rpcMessage, broadcastData, broadcastChannel)
              }
              case None => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Server failed to extract chirp id for the broadcast", rpcMessage.id))
            }
          }
          case None => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Server failed to extract LAO id for the broadcast", rpcMessage.id))
        }
      }
      case error@Right(_) =>
        error
      case _ =>
        Right(PipelineError(ErrorCodes.SERVER_ERROR.id, unknownAnswerDatabase, rpcMessage.id))
    }
  }

  // no need for a case handleNotifyAddChirp or handleNotifyDeleteChirp for now, since the server never receives any in theory, but could be needed later
  def handleNotifyAddChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
    Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED: SocialMediaHandler should not handle NotifyAddChirp messages", rpcMessage.id))
  }

  def handleNotifyDeleteChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
    Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED: SocialMediaHandler should not handle NotifyDeleteChirp messages", rpcMessage.id))
  }

}

