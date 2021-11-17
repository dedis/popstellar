package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.{Channel, PublicKey}
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.dataObject.LaoData
import ch.epfl.pop.model.network.requests.rollCall.{JsonRpcRequestCloseRollCall, JsonRpcRequestCreateRollCall, JsonRpcRequestOpenRollCall, JsonRpcRequestReopenRollCall}
import ch.epfl.pop.model.network.method.message.data.rollCall.CloseRollCall
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.pubsub.graph.{DbActor, ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.pubsub.graph.DbActor._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

case object RollCallHandler extends MessageHandler {

  override val handler: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Left(jsonRpcMessage) => jsonRpcMessage match {
      case message@(_: JsonRpcRequestCreateRollCall) => handleCreateRollCall(message)
      case message@(_: JsonRpcRequestOpenRollCall) => handleOpenRollCall(message)
      case message@(_: JsonRpcRequestReopenRollCall) => handleReopenRollCall(message)
      case message@(_: JsonRpcRequestCloseRollCall) => handleCloseRollCall(message)
      case _ => Right(PipelineError(
        ErrorCodes.SERVER_ERROR.id,
        "Internal server fault: RollCallHandler was given a message it could not recognize",
        jsonRpcMessage match {
          case r: JsonRpcRequest => r.id
          case r: JsonRpcResponse => r.id
          case _ => None
        }
      ))
    }
    case graphMessage@_ => graphMessage
  }

  def handleCreateRollCall(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }

  def handleOpenRollCall(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }

  def handleReopenRollCall(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }

  def handleCloseRollCall(rpcMessage: JsonRpcRequest): GraphMessage = {
    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: CloseRollCall = message.decodedData.get.asInstanceOf[CloseRollCall]
        val unknownAnswer: String = "Database actor returned an unknown answer"

        //Creates a channel for each attendee (of name /root/lao_id/social/PublicKeyAttendee), returns a GraphMessage
        def createAttendeeChannels(attendees: List[PublicKey], rpcMessage: JsonRpcRequest): GraphMessage = {
          attendees match {
            case Nil => Left(rpcMessage)
            case head::tail => 
              //a closeRollCall is always sent in the lao's main channel so we are allowed to do this
              val socialChannel: String = rpcMessage.getParamsChannel + "social/" + head.toString
              val ask: Future[GraphMessage] = (dbActor ? DbActor.CreateChannel(Channel(socialChannel), ObjectType.CHIRP)).map {
                case DbActorWriteAck() => createAttendeeChannels(tail, rpcMessage)
                //FIXME: Once the real PoP tokens are implemented, the same person will not have access to the same channel name twice in a row, so we can remove this line
                //however, this is to prevent errors for now within a LAO for multiple Roll Calls and the same participants since the public key will probably not change
                case DbActorNAck(code, description) if description == "Database cannot create an already existing channel (" + socialChannel + ")" => createAttendeeChannels(tail, rpcMessage)
                case DbActorNAck(code, description) => Right(PipelineError(code, description, rpcMessage.id))
                case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, unknownAnswer, rpcMessage.id))
              }
              Await.result(ask, duration)
          }
        }

        val laoChannel: Option[Array[Byte]] = rpcMessage.getParamsChannel.decodeSubChannel
        laoChannel match {
          case Some(channel) =>
          case None => Right(PipelineError(
            ErrorCodes.SERVER_ERROR.id,
            s"There is an issue with the data of the LAO",
            rpcMessage.id
          ))
        }

        val askOldData = dbActor ? DbActor.ReadLaoData(rpcMessage.getParamsChannel)
        
        Await.result(askOldData, duration) match {
          case DbActorReadLaoDataAck(Some(oldLaoData)) =>
            val laoData: LaoData = LaoData(oldLaoData.owner, data.attendees)
            val ask: Future[GraphMessage] = (dbActor ? DbActor.WriteLaoData(rpcMessage.getParamsChannel, message, laoData)).map {
              case DbActorWriteAck() => createAttendeeChannels(data.attendees, rpcMessage)
              case DbActorNAck(code, description) => Right(PipelineError(code, description, rpcMessage.id))
              case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, unknownAnswer, rpcMessage.id))
            }
            Await.result(ask, duration)
          case _ => Right(PipelineError(
            ErrorCodes.SERVER_ERROR.id,
            s"There is an issue with the data of the LAO",
            rpcMessage.id
          ))

        }
      case _ => Right(PipelineError(
        ErrorCodes.SERVER_ERROR.id,
        s"Unable to handle lao message $rpcMessage. Not a Publish/Broadcast message",
        rpcMessage.id
      ))
    }
  }
}
