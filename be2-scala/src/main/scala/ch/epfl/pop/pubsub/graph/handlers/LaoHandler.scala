package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.lao.{CreateLao, StateLao}
import ch.epfl.pop.model.network.requests.lao.{JsonRpcRequestCreateLao, JsonRpcRequestStateLao, JsonRpcRequestUpdateLao}
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.model.objects.{Channel, Hash}
import ch.epfl.pop.pubsub.graph.DbActor.{DbActorNAck, DbActorWriteAck}
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

  def handleCreateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: CreateLao = message.decodedData.get.asInstanceOf[CreateLao]
        // we are using the lao id instead of the message_id at lao creation
        val channel: Channel = Channel(s"${Channel.rootChannelPrefix}${data.id}")

        val ask: Future[GraphMessage] = (dbActor ? DbActor.Write(channel, message)).map {
          case DbActorWriteAck => Left(rpcMessage)
          case DbActorNAck(code, description) => Right(PipelineError(code, description, rpcMessage.id))
          case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Database actor returned an unknown answer", rpcMessage.id))
        }

        Await.result(ask, duration)

      case _ => Right(PipelineError(
        ErrorCodes.SERVER_ERROR.id,
        s"Unable to handle lao message $rpcMessage. Not a Publish/Broadcast message",
        rpcMessage.id
      ))
    }
  }

  def handleStateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    val modificationId: Hash = rpcMessage.getDecodedData.asInstanceOf[StateLao].modification_id
    // val ask = dbActor.ask(ref => DbActor.Read(rpcMessage.getParamsChannel, modificationId, ref)).map {
    val ask = dbActor.ask("TODO").map {
      case Some(_) =>
        val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
        Await.result(ask, duration)
      // TODO careful about asynchrony and the fact that the network may reorder some messages
      case _ => Right(PipelineError(
        ErrorCodes.INVALID_DATA.id,
        s"Unable to request lao state: invalid modification_id '$modificationId' (no message associated to this id)",
        rpcMessage.id
      ))
    }
    Await.result(ask, duration)
  }

  def handleUpdateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }
}
