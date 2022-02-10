package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.{Base64Data, Channel, PublicKey, LaoData}
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.requests.rollCall.{JsonRpcRequestCloseRollCall, JsonRpcRequestCreateRollCall, JsonRpcRequestOpenRollCall, JsonRpcRequestReopenRollCall}
import ch.epfl.pop.model.network.method.message.data.rollCall.CloseRollCall
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.pubsub.graph.{DbActor, ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.pubsub.graph.DbActor._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

import scala.util.Success
import akka.pattern.AskableActorRef

/**
 * RollCallHandler object uses the db instance from the MessageHandler (i.e PublishSubscribe)
 */
object RollCallHandler extends MessageHandler {
  override val handler = new RollCallHandler(super.dbActor).handler
}

/**
  * Implementation of the RollCallHandler that provides a testable interface
  * @param dbRef reference of the db actor
  */
sealed class RollCallHandler(dbRef: => AskableActorRef) extends MessageHandler {

  /**
    * Overrides default DbActor with provided parameter
    */
  override final val dbActor: AskableActorRef = dbRef

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

  private val unknownAnswer: String = "Database actor returned an unknown answer"

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
    // We first create the three (independent) futures
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)

    // Creates a channel for each attendee (of name /root/lao_id/social/PublicKeyAttendee), returns a Future[GraphMessage]
    def createAttendeeChannels(data: CloseRollCall, rpcMessage: JsonRpcRequest): Future[GraphMessage] = {
      val listAttendeeChannels: List[(Channel, ObjectType.ObjectType)] = data.attendees.map(attendee => (generateSocialChannel(rpcMessage.getParamsChannel, attendee), ObjectType.CHIRP))
      val askCreateChannels: Future[GraphMessage] = (dbActor ? DbActor.CreateChannelsFromList(listAttendeeChannels)).map{
        case DbActorAck() => Left(rpcMessage)
        case DbActorNAck(code, description) => Right(PipelineError(code, description, rpcMessage.id))
        case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, unknownAnswer, rpcMessage.id))
      }
      askCreateChannels
    }

    val askOldData = dbActor ? DbActor.ReadLaoData(rpcMessage.getParamsChannel)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: CloseRollCall = message.decodedData.get.asInstanceOf[CloseRollCall]
        val askCreateChannels: Future[GraphMessage] = createAttendeeChannels(data, rpcMessage)

        // We create a new Future (tuple of Futures) to remove the obligatory sequential execution
        // TODO: A possible addition would be to handle the error messages better
        val resFuture: Future[GraphMessage] = (for{
          f1 <- ask
          f2 <- askOldData
          f3 <- askCreateChannels
        } yield(f1, f2, f3)).map{
          case (Left(_), DbActorReadLaoDataAck(Some(_)), Left(_)) => Left(rpcMessage)
          case (Left(_), DbActorNAck(code, description), Left(_)) => Right(PipelineError(code, description, rpcMessage.id))
          case (Left(_), _, error@Right(_)) => error
          case (error@Right(_), _, _) => error
          case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, unknownAnswer, rpcMessage.id))
        }

        val laoChannel: Option[Base64Data] = rpcMessage.getParamsChannel.decodeChannelLaoId
        laoChannel match {
          case None => Right(PipelineError(
            ErrorCodes.SERVER_ERROR.id,
            s"There is an issue with the data of the LAO",
            rpcMessage.id
          ))
          case Some(_) => Await.result(resFuture, duration)
        }
      
      case _ => Right(PipelineError(
            ErrorCodes.SERVER_ERROR.id,
            s"Unable to handle lao message $rpcMessage. Not a Publish/Broadcast message",
            rpcMessage.id
          ))
    }

    /*Await.result(ask, duration) match {
      case Left(msg) => {
        rpcMessage.getParamsMessage match {
          case Some(message: Message) =>
            val data: CloseRollCall = message.decodedData.get.asInstanceOf[CloseRollCall]

            //Creates a channel for each attendee (of name /root/lao_id/social/PublicKeyAttendee), returns a GraphMessage
            def createAttendeeChannels(attendees: List[PublicKey], rpcMessage: JsonRpcRequest): GraphMessage = {
              val listAttendeeChannels: List[(Channel, ObjectType.ObjectType)] = data.attendees.map(attendee => (generateSocialChannel(rpcMessage.getParamsChannel, attendee), ObjectType.CHIRP))
              val askCreateChannels: Future[GraphMessage] = (dbActor ? DbActor.CreateChannelsFromList(listAttendeeChannels)).map{
                case DbActorAck() => Left(rpcMessage)
                case DbActorNAck(code, description) => Right(PipelineError(code, description, rpcMessage.id))
                case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, unknownAnswer, rpcMessage.id))
              }
              Await.result(askCreateChannels, duration)
            }

            val laoChannel: Option[Base64Data] = rpcMessage.getParamsChannel.decodeChannelLaoId
            laoChannel match {
              case None => Right(PipelineError(
                ErrorCodes.SERVER_ERROR.id,
                s"There is an issue with the data of the LAO",
                rpcMessage.id
              ))
              case Some(_) =>
                val askOldData = dbActor ? DbActor.ReadLaoData(rpcMessage.getParamsChannel)

                Await.result(askOldData, duration) match {
                  case DbActorReadLaoDataAck(Some(_)) =>
                    val ask: Future[GraphMessage] = (dbActor ? DbActor.Write(rpcMessage.getParamsChannel, message)).map {
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
            }
          case _ => Right(PipelineError(
            ErrorCodes.SERVER_ERROR.id,
            s"Unable to handle lao message $rpcMessage. Not a Publish/Broadcast message",
            rpcMessage.id
          ))
        }
      }
      case error@Right(_) => error
      case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, unknownAnswer, rpcMessage.id))
    }*/
  }

  private def generateSocialChannel(channel: Channel, pk: PublicKey): Channel = Channel(channel + Channel.SOCIAL_CHANNEL_PREFIX + pk.base64Data)
}
