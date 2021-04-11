package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.lao.{CreateLao, StateLao}
import ch.epfl.pop.model.network.requests.lao.{JsonRpcRequestCreateLao, JsonRpcRequestStateLao, JsonRpcRequestUpdateLao}
import ch.epfl.pop.model.objects.{Channel, Hash}
import ch.epfl.pop.pubsub.ChannelActor.CreateMessage
import ch.epfl.pop.pubsub.graph.{DbActorNew, ErrorCodes, GraphMessage, PipelineError}

import scala.util.Success

case object LaoHandler extends MessageHandler {

  override val handler: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Left(jsonRpcMessage) => jsonRpcMessage match {
      case message@(_: JsonRpcRequestCreateLao) => handleCreateLao(message)
      case message@(_: JsonRpcRequestStateLao) => handleStateLao(message)
      case message@(_: JsonRpcRequestUpdateLao) => handleUpdateLao(message)
      case _ => Right(PipelineError(
        ErrorCodes.SERVER_FAULT.id,
        "Internal server fault: LaoHandler was given a message it could not recognize"
      ))
    }
    case graphMessage@_ => graphMessage
  }

  def handleCreateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    val messageData: CreateLao = CreateLao.buildFromPartial(rpcMessage.getDecodedData.get, rpcMessage)
    val channel: String = s"${Channel.rootChannelPrefix}${messageData.id}"

    subActor.ask(ref => CreateMessage(channel, ref)) match {
      case Success(_) =>
        // Publish on the LAO main channel
        dbActor.ask(ref => DbActorNew.Write(channel, rpcMessage.getParamsMessage.get, ref)) match {
          case Success(_) => Left(rpcMessage)
          case _ => Right(PipelineError(-10, "")) // FIXME add DbActor "answers" with error description if failed
        }
      case _ => Right(PipelineError(ErrorCodes.ALREADY_EXISTS.id, s"Unable to create lao: channel '$channel' already exists"))
    }
  }

  def handleStateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    val modificationId: Hash = rpcMessage.getDecodedData.asInstanceOf[StateLao].modification_id
    dbActor.ask(ref => DbActorNew.Read(rpcMessage.getParamsChannel, modificationId, ref)) match {
      case Some(_) => dbAskWritePropagate(rpcMessage)
      case _ => Right(PipelineError(
        ErrorCodes.INVALID_DATA.id,
        s"Unable to request lao state: invalid modification_id '$modificationId' (no message associated to this id)"
      ))
    }
  }

  def handleUpdateLao(rpcMessage: JsonRpcRequest): GraphMessage = dbAskWritePropagate(rpcMessage)
}
