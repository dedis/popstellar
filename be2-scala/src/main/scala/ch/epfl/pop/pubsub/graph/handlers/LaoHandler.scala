package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.lao.{CreateLao, StateLao}
import ch.epfl.pop.model.network.requests.lao.{JsonRpcRequestCreateLao, JsonRpcRequestStateLao, JsonRpcRequestUpdateLao}
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.model.objects.{Channel, DbActorNAckException, Hash, LaoData}
import ch.epfl.pop.pubsub.graph.DbActor.{DbActorMessage, DbActorAck, DbActorWriteAck}
import ch.epfl.pop.pubsub.graph.{DbActor, ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.pubsub.graph.validators.SocialMediaValidator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.Failure

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

  private val unknownAnswerDB: String = "Database actor returned an unknown answer"

  def handleCreateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: CreateLao = message.decodedData.get.asInstanceOf[CreateLao]
        // we are using the lao id instead of the message_id at lao creation
        val channel: Channel = Channel(s"${Channel.ROOT_CHANNEL_PREFIX}${data.id}")
        val socialChannel: Channel = Channel(channel + Channel.SOCIAL_MEDIA_CHIRPS_PREFIX)
        val reactionChannel: Channel = Channel(channel + Channel.REACTIONS_CHANNEL_PREFIX)

        val askCreateChannels = (dbActor ? DbActor.CreateChannelsFromList(List(
          (channel, ObjectType.LAO),
          (socialChannel, ObjectType.CHIRP),
          (reactionChannel, ObjectType.REACTION)
        )))
        val askWrite = (dbActor ? DbActor.Write(channel, message))

        val resFuture: Future[GraphMessage] = (for{
          resultCreateChannels <- askCreateChannels
          resultWrite <- askWrite
        } yield(resultWrite, resultCreateChannels)).map{
          case (DbActorWriteAck(), DbActorAck()) => Left(rpcMessage)
          case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, unknownAnswerDB, rpcMessage.id))
        }.recover{
          case e: DbActorNAckException => Right(PipelineError(e.getCode, e.getMessage, rpcMessage.id))
          case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, unknownAnswerDB, rpcMessage.id))
        }

        Await.result(resFuture, duration)

      case _ => Right(PipelineError(
        ErrorCodes.SERVER_ERROR.id,
        s"Unable to handle lao message $rpcMessage. Not a Publish/Broadcast message",
        rpcMessage.id
      ))
    }
  }

  def handleStateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    val modificationId: Hash = rpcMessage.getDecodedData.asInstanceOf[StateLao].modification_id
    Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED : handleStateLao is not implemented", rpcMessage.id))
  }

  def handleUpdateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    //FIXME: the main channel is not updated
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }
}
