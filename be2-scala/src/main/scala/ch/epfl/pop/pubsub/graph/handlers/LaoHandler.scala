package ch.epfl.pop.pubsub.graph.handlers

import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.lao.{CreateLao, StateLao}
import ch.epfl.pop.model.network.requests.lao.{JsonRpcRequestCreateLao, JsonRpcRequestStateLao, JsonRpcRequestUpdateLao}
import ch.epfl.pop.model.objects.{Channel, Hash}
import ch.epfl.pop.pubsub.ChannelActor.CreateMessage
import ch.epfl.pop.pubsub.graph.{DbActorNew, ErrorCodes, GraphMessage, PipelineError}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

case object LaoHandler extends MessageHandler {

  override val handler: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Left(jsonRpcMessage) => jsonRpcMessage match {
      case message@(_: JsonRpcRequestCreateLao) => handleCreateLao(message)
      case message@(_: JsonRpcRequestStateLao) => handleStateLao(message)
      case message@(_: JsonRpcRequestUpdateLao) => handleUpdateLao(message)
      case _ => Right(PipelineError(
        ErrorCodes.SERVER_ERROR.id,
        "Internal server fault: LaoHandler was given a message it could not recognize"
      ))
    }
    case graphMessage@_ => graphMessage
  }

  def handleCreateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    val messageData: CreateLao = CreateLao.buildFromPartial(rpcMessage.getDecodedData.get, rpcMessage)
    val channel: Channel = Channel(s"${Channel.rootChannelPrefix}${messageData.id}")

    val subActor: AskableActorRef = ??? // FIXME temporary for the project to compile. Should be mnodified when subscribe/unsubscribe implemented
    val ask = subActor.ask(ref => CreateMessage(channel.channel, ref)).map {
      case true =>
        // Publish on the LAO main channel
        val ask = dbActor.ask(ref => DbActorNew.Write(channel, rpcMessage.getParamsMessage.get, ref)).map {
          case true => Left(rpcMessage)
          case _ => Right(PipelineError(-10, "")) // FIXME add DbActor "answers" with error description if failed
        }
        Await.result(ask, DbActorNew.getDuration)
      case _ => Right(PipelineError(ErrorCodes.ALREADY_EXISTS.id, s"Unable to create lao: channel '$channel' already exists"))
    }
    Await.result(ask, Duration(1, TimeUnit.SECONDS))
  }

  def handleStateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    val modificationId: Hash = rpcMessage.getDecodedData.asInstanceOf[StateLao].modification_id
    val ask = dbActor.ask(ref => DbActorNew.Read(rpcMessage.getParamsChannel, modificationId, ref)).map {
      case Some(_) => dbAskWritePropagate(rpcMessage)
      // TODO careful about asynchrony and the fact that the network may reorder some messages
      case _ => Right(PipelineError(
        ErrorCodes.INVALID_DATA.id,
        s"Unable to request lao state: invalid modification_id '$modificationId' (no message associated to this id)"
      ))
    }
    Await.result(ask, DbActorNew.getDuration)
  }

  def handleUpdateLao(rpcMessage: JsonRpcRequest): GraphMessage = dbAskWritePropagate(rpcMessage)
}
