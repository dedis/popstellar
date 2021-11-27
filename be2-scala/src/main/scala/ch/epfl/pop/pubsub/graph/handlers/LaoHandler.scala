package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.lao.{CreateLao, StateLao}
import ch.epfl.pop.model.network.requests.lao.{JsonRpcRequestCreateLao, JsonRpcRequestStateLao, JsonRpcRequestUpdateLao}
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.model.objects.{Channel, Hash, LaoData}
import ch.epfl.pop.pubsub.graph.DbActor.{DbActorMessage, DbActorAck, DbActorNAck, DbActorWriteAck}
import ch.epfl.pop.pubsub.graph.{DbActor, ErrorCodes, GraphMessage, PipelineError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

case object LaoHandler extends MessageHandler {

  override val handler: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Left(jsonRpcMessage) => jsonRpcMessage match {
      case message@(_: JsonRpcRequestCreateLao) => handleCreateLao(message)
      case message@(_: JsonRpcRequestStateLao) => handleStateLao(message)
      case message@(_: JsonRpcRequestUpdateLao) => handleUpdateLao(message)
      case _ => Right(PipelineError(
        ErrorCodes.SERVER_ERROR.id,
        "Internal server fault: LaoHandler was given a message it could not recognize",
        jsonRpcMessage match {
          case r: JsonRpcRequest => r.id
          case r: JsonRpcResponse => r.id
          case _ => None
        }
      ))
    }
    case graphMessage@_ => graphMessage
  }

  private final val SOCIALMEDIAPOSTSPREFIX: String = Channel.SEPARATOR + "posts"

  def handleCreateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: CreateLao = message.decodedData.get.asInstanceOf[CreateLao]
        // we are using the lao id instead of the message_id at lao creation
        val channel: Channel = Channel(s"${Channel.rootChannelPrefix}${data.id}")
        //this prevents a write-through to an already existing LaoId
        val askCreate = (dbActor ? DbActor.CreateChannel(channel, ObjectType.LAO))
        Await.result(askCreate, duration) match {
          case DbActorNAck(code, description) => Right(PipelineError(code, description, rpcMessage.id))
          case DbActorAck() => {
            val ask: Future[GraphMessage] = (dbActor ? DbActor.Write(channel, message)).map {
              case DbActorWriteAck() => {
                val socialChannel: Channel = Channel(channel + SOCIALMEDIAPOSTSPREFIX)
                val askSocial: Future[GraphMessage] = (dbActor ? DbActor.CreateChannel(socialChannel, ObjectType.CHIRP)).map {
                  case DbActorAck() => Left(rpcMessage)
                  case DbActorNAck(code, description) => Right(PipelineError(code, description, rpcMessage.id))
                  case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Database actor returned an unknown answer", rpcMessage.id))
                }
                Await.result(askSocial, duration)
              }
              case DbActorNAck(code, description) => Right(PipelineError(code, description, rpcMessage.id))
              case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Database actor returned an unknown answer", rpcMessage.id))
            }

            Await.result(ask, duration)
          }
        }

      case _ => Right(PipelineError(
        ErrorCodes.SERVER_ERROR.id,
        s"Unable to handle lao message $rpcMessage. Not a Publish/Broadcast message",
        rpcMessage.id
      ))
    }
  }

  def handleStateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    val modificationId: Hash = rpcMessage.getDecodedData.asInstanceOf[StateLao].modification_id
    Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED : handleStateMeeting is not implemented", rpcMessage.id))
  }

  def handleUpdateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    //FIXME: the main channel is not updated
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }
}
