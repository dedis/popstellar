package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.socialMedia.{AddChirp, AddBroadcastChirp}
import ch.epfl.pop.model.network.requests.socialMedia.{JsonRpcRequestAddChirp}
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.model.objects.{Channel, Hash, Base64Data, Signature, Timestamp, PublicKey}
import ch.epfl.pop.pubsub.graph.{DbActor, ErrorCodes, GraphMessage, PipelineError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

import com.google.crypto.tink.subtle.Ed25519Sign

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
    //broadcast chirp too? On general channel?


    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration) match {
      case Success(Left(msg)) => {
        val channelChirp: Channel = rpcMessage.getParamsChannel
        channelChirp.decodeSubChannel match {
          case(Some(lao_id)) => {
            val broadcastChannel: String = rootChannelPrefix + lao_id + "/path"
            rpcMessage.getParamsMessage match {
              case Some(params) => {
                // we can't get the message_id as a Base64Data, it is a Hash
                val chirpId: Hash = params.message_id
                val timestamp: Timestamp = params.getDecodedData.get.asInstanceOf[AddChirp].timestamp
                val addBroadcastChirp: AddBroadcastChirp = AddBroadcastChirp(chirpId, channelChirp, timestamp)
                val broadcastData: Base64Data = Base64Data(addBroadcastChirp.toString) //FIXME: check json/string conversion
                //Ed25519 is used to verify the signatures, therefore I also use it to sign data
                //FIXME: however, should we have the server generating a new keypair each time? Because it doesn't feel very secure to me.
                //TODO: add new object keypair to LaoData (generated during LAO creation) and store it there
                val keypair: Ed25519Sign.KeyPair = Ed25519Sign.KeyPair.newKeyPair()
                val pk: PublicKey = PublicKey(Base64Data.encode(keypair.getPublicKey)) // check where to generate: keypair = Ed25519Sign.KeyPair.newKeyPair() (keypair.getPublicKey())
                val ed: Ed25519Sign = Ed25519Sign(keypair.getPrivateKey)
                val broadcastSignature: Signature = Signature(Base64Data.encode(ed.sign(broadcastData.decode)))
                val broadcastId: Hash = Hash.fromStrings(broadcastData.toString, broadcastSignature.toString)
                val broadcastMessage: Message = Message(broadcastData, pk, broadcastSignature, broadcastId, List.empty)
                val ask: Future[GraphMessage] = (dbActor ? DbActor.WriteAndPropagate(broadcastChannel, broadcastMessage)).map {
                  case DbActor.DbActorWriteAck() => Left(msg)
                  case DbActor.DbActorNAck(code, description) => Right(PipelineError(code, description, rpcMessage.id))
                  case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Database actor returned an unknown answer", rpcMessage.id))
                }
                Await.result(ask, duration)

              }
              case None => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Server failed to extract chirp id for the broadcast", rpcMessage.id))
            }
          }
          case None => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Server failed to extract LAO id for the broadcast", rpcMessage.id))
        }
      }
      case Success(Right(error)) =>
        error
      case _ =>
        Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Database actor returned an unknown answer", rpcMessage.id))
    }
    
  }

  // no need for a case handleAddBroadcastChirp, since the server never receives one in theory


}

