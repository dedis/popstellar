package ch.epfl.pop.pubsub.graph.handlers

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.rollCall.{CloseRollCall, CreateRollCall}
import ch.epfl.pop.model.objects.{Channel, DbActorNAckException, Hash, PublicKey}
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

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
class RollCallHandler(dbRef: => AskableActorRef) extends MessageHandler {

  /**
   * Overrides default DbActor with provided parameter
   */
  override final val dbActor: AskableActorRef = dbRef

  private val unknownAnswer: String = "Database actor returned an unknown answer"

  def handleCreateRollCall(rpcRequest: JsonRpcRequest): GraphMessage = {
    rpcRequest.getParamsMessage match {
      case Some(message: Message) =>
        val data: CreateRollCall = message.decodedData.get.asInstanceOf[CreateRollCall]
        // we are using the rollcall id instead of the message_id at rollcall creation
        val rollCallChannel: Channel = Channel(s"${Channel.ROOT_CHANNEL_PREFIX}${data.id}")
        val ask =
          for {
            _ <- dbActor ? DbActor.ChannelExists(rollCallChannel) transformWith {
              case Success(_) => Future {
                throw DbActorNAckException(ErrorCodes.INVALID_ACTION.id, "rollCall already exists in db")
              }
              case _ => Future {}
            }
            _ <- dbActor ? DbActor.CreateChannel(rollCallChannel, ObjectType.ROLL_CALL)
            _ <- dbAskWritePropagate(rpcRequest)
          } yield ()

        Await.ready(ask, duration).value match {
          case Some(Success(_)) => Left(rpcRequest)
          case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"handleCreateRollCall failed : ${ex.message}", rpcRequest.getId))
          case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleCreateRollCall failed : unexpected DbActor reply '$reply'", rpcRequest.getId))
        }
      case _ =>
        Right(
          PipelineError(ErrorCodes.SERVER_ERROR.id, "The server is doing something unexpected", rpcRequest.getId)
        )
    }
  }

  def handleOpenRollCall(rpcRequest: JsonRpcRequest): GraphMessage = {
    //check if the roll call already exists to open it
    val ask =
      for (
        _ <- dbActor ? DbActor.ChannelExists(rpcRequest.getParamsChannel) transformWith {
          case Success(_) => Future { () }
          case _ => Future { throw DbActorNAckException(ErrorCodes.INVALID_ACTION.id, "rollCall does not exist in db") }
        };
        _ <- dbAskWritePropagate(rpcRequest)
      ) yield ()

    Await.ready(ask, duration).value match {
      case Some(Success(_)) => Left(rpcRequest)
      case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"handleOpenRollCall failed : ${ex.message}", rpcRequest.getId))
      case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleOpenRollCall failed : unexpected DbActor reply '$reply'", rpcRequest.getId))
    }
  }

  def handleReopenRollCall(rpcRequest: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcRequest)
    Await.result(ask, duration)
  }

  def handleCloseRollCall(rpcRequest: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcRequest)
    Await.result(ask, duration) match {
      case Left(_) =>
        rpcRequest.getParamsMessage match {
          case Some(message: Message) =>
            val data: CloseRollCall = message.decodedData.get.asInstanceOf[CloseRollCall]

            // creates a channel for each attendee (of name /root/lao_id/social/PublicKeyAttendee), returns a GraphMessage
            def createAttendeeChannels(attendees: List[PublicKey]): GraphMessage = {
              val listAttendeeChannels: List[(Channel, ObjectType.ObjectType)] = data.attendees.map {
                attendee => (generateSocialChannel(rpcRequest.getParamsChannel, attendee), ObjectType.CHIRP)
              }

              val askCreateChannels = dbActor ? DbActor.CreateChannelsFromList(listAttendeeChannels)

              Await.ready(askCreateChannels, duration).value match {
                case Some(Success(_)) => Left(rpcRequest)
                case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"handleCloseRollCall failed : ${ex.message}", rpcRequest.getId))
                case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleCloseRollCall failed : unexpected DbActor reply '$reply'", rpcRequest.getId))
              }
            }
            val laoChannel: Option[Hash] = rpcRequest.getParamsChannel.decodeChannelLaoId
            laoChannel match {
              case None => Right(PipelineError(
                ErrorCodes.SERVER_ERROR.id,
                s"There is an issue with the data of the LAO",
                rpcRequest.id
              ))
              case Some(_) =>
                val combined = for {
                  _ <- dbActor ? DbActor.ReadLaoData(rpcRequest.getParamsChannel)
                  _ <- dbActor ? DbActor.WriteLaoData(rpcRequest.getParamsChannel, message, None)
                } yield ()

                Await.ready(combined, duration).value match {
                  case Some(Success(_)) => createAttendeeChannels(data.attendees)
                  case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"handleCloseRollCall failed : ${ex.message}", rpcRequest.getId))
                  case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleCloseRollCall failed : unexpected DbActor reply '$reply'", rpcRequest.getId))
                }
            }
          case _ => Right(PipelineError(
            ErrorCodes.SERVER_ERROR.id,
            s"Unable to handle lao message $rpcRequest. Not a Publish/Broadcast message",
            rpcRequest.id
          ))
        }
      case error@Right(_) => error
      case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, unknownAnswer, rpcRequest.id))
    }
  }

  private def generateSocialChannel(channel: Channel, pk: PublicKey): Channel = Channel(s"$channel${Channel.SOCIAL_CHANNEL_PREFIX}${pk.base64Data}")
}
