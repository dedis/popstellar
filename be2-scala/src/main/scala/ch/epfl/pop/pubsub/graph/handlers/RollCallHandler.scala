package ch.epfl.pop.pubsub.graph.handlers

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.rollCall.CloseRollCall
import ch.epfl.pop.model.objects.{Base64Data, Channel, PublicKey}
import ch.epfl.pop.pubsub.graph.DbActor._
import ch.epfl.pop.pubsub.graph.{DbActor, ErrorCodes, GraphMessage, PipelineError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

/**
 * RollCallHandler object uses the db instance from the MessageHandler (i.e PublishSubscribe)
 */
object RollCallHandler extends MessageHandler {
  final lazy val handlerInstance = new RollCallHandler(super.dbActor)

  def handleCreateRollCall(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleCreateRollCall(rpcMessage)
  def handleOpenRollCall(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleOpenRollCall(rpcMessage)
  def handleReopenRollCall(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleReopenRollCall(rpcMessage)
  def handleCloseRollCall(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleCloseRollCall(rpcMessage)
}

/**
 * Implementation of the RollCallHandler that provides a testable interface
 *
 * @param dbRef reference of the db actor
 */
sealed class RollCallHandler(dbRef: => AskableActorRef) extends MessageHandler {

  /**
   * Overrides default DbActor with provided parameter
   */
  override final val dbActor: AskableActorRef = dbRef

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
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration) match {
      case Left(_) =>
        rpcMessage.getParamsMessage match {
          case Some(message: Message) =>
            val data: CloseRollCall = message.decodedData.get.asInstanceOf[CloseRollCall]

            //Creates a channel for each attendee (of name /root/lao_id/social/PublicKeyAttendee), returns a GraphMessage
            def createAttendeeChannels(attendees: List[PublicKey], rpcMessage: JsonRpcRequest): GraphMessage = {
              val listAttendeeChannels: List[(Channel, ObjectType.ObjectType)] = data.attendees.map(attendee => (generateSocialChannel(rpcMessage.getParamsChannel, attendee), ObjectType.CHIRP))
              val askCreateChannels: Future[GraphMessage] = (dbActor ? DbActor.CreateChannelsFromList(listAttendeeChannels)).map {
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
      case error@Right(_) => error
      case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, unknownAnswer, rpcMessage.id))
    }
  }

  private def generateSocialChannel(channel: Channel, pk: PublicKey): Channel = Channel(s"$channel${Channel.SOCIAL_CHANNEL_PREFIX}${pk.base64Data}")
}
