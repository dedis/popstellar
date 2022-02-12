package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.method.message.data.meeting.StateMeeting
import ch.epfl.pop.model.network.requests.meeting.{JsonRpcRequestCreateMeeting, JsonRpcRequestStateMeeting}
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.model.objects.Hash
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}

import scala.concurrent.{Await, Future}

case object MeetingHandler extends MessageHandler {

  override val handler: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Left(jsonRpcMessage) => jsonRpcMessage match {
      case message@(_: JsonRpcRequestCreateMeeting) => handleCreateMeeting(message)
      case message@(_: JsonRpcRequestStateMeeting) => handleStateMeeting(message)
      case _ => Right(PipelineError(
        ErrorCodes.SERVER_ERROR.id,
        "Internal server fault: MeetingHandler was given a message it could not recognize",
        jsonRpcMessage match {
          case r: JsonRpcRequest => r.id
          case r: JsonRpcResponse => r.id
          case _ => None
        }
      ))
    }
    case graphMessage@_ => graphMessage
  }

  def handleCreateMeeting(rpcMessage: JsonRpcRequest): GraphMessage = {
    rpcMessage.getParamsChannel.decodeChannelLaoId match {
      case Some(_) =>
        val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
        Await.result(ask, duration)
      case _ => Right(PipelineError(
        ErrorCodes.INVALID_DATA.id,
        s"Unable to create meeting: invalid encoded laoId '${rpcMessage.getParamsChannel}'",
        rpcMessage.id
      ))
    }
  }

  def handleStateMeeting(rpcMessage: JsonRpcRequest): GraphMessage = {
    val modificationId: Hash = rpcMessage.getDecodedData.asInstanceOf[StateMeeting].modification_id
    Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED : handleStateMeeting is not implemented", rpcMessage.id))
  }
}
