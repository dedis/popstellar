package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.socialMedia.{AddChirp, AddBroadcastChirp}
import ch.epfl.pop.model.network.requests.socialMedia.{JsonRpcRequestAddChirp}
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
  
  def handleAddChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration) match {
      case Left(msg) => {
        val channelChirp: Channel = rpcMessage.getParamsChannel
        channelChirp.decodeSubChannel match {
          case(Some(lao_id)) => {
            val broadcastChannel: Channel = Channel(Channel.rootChannelPrefix + Base64Data.encode(lao_id) + Channel.SOCIALMEDIAPOSTSPREFIX)
            rpcMessage.getParamsMessage match {
              case Some(params) => {
                // we can't get the message_id as a Base64Data, it is a Hash
                val chirp_id: Hash = params.message_id
                val timestamp: Timestamp = params.decodedData.get.asInstanceOf[AddChirp].timestamp
                val addBroadcastChirp: AddBroadcastChirp = AddBroadcastChirp(chirp_id, channelChirp, timestamp)
                val broadcastData: Base64Data = Base64Data.encode(addBroadcastChirp.toJson.toString)
                
                val askLaoData = (dbActor ? DbActor.ReadLaoData(rpcMessage.getParamsChannel))
                Await.result(askLaoData, duration) match {
                  case DbActor.DbActorReadLaoDataAck(Some(laoData)) => {
                    val broadcastSignature: Signature = laoData.privateKey.signData(broadcastData)
                    val broadcastId: Hash = Hash.fromStrings(broadcastData.toString, broadcastSignature.toString)
                    val broadcastMessage: Message = Message(broadcastData, laoData.publicKey, broadcastSignature, broadcastId, List.empty)
                    val ask: Future[GraphMessage] = (dbActor ? DbActor.WriteAndPropagate(broadcastChannel, broadcastMessage)).map {
                      case DbActor.DbActorWriteAck() => Left(msg)
                      case DbActor.DbActorNAck(code, description) => Right(PipelineError(code, description, rpcMessage.id))
                      case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Database actor returned an unknown answer", rpcMessage.id))
                    }
                    Await.result(ask, duration)
                  }
                  case DbActor.DbActorReadLaoDataAck(None) => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Can't fetch LaoData", rpcMessage.id))
                  case DbActor.DbActorNAck(code, description) => Right(PipelineError(code, description, rpcMessage.id))
                }
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
        Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Database actor returned an unknown answer", rpcMessage.id))
    }
    
  }

  // no need for a case handleAddBroadcastChirp, since the server never receives one in theory


}

