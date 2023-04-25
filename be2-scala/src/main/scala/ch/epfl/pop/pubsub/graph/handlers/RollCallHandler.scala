package ch.epfl.pop.pubsub.graph.handlers

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.rollCall.{CloseRollCall, CreateRollCall, OpenRollCall, ReopenRollCall}
import ch.epfl.pop.model.objects.{Channel, DbActorNAckException, Hash, PublicKey}
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.util.{Failure, Success}

/** RollCallHandler object uses the db instance from the MessageHandler (i.e PublishSubscribe)
  */
object RollCallHandler extends MessageHandler {
  final lazy val handlerInstance = new RollCallHandler(super.dbActor)

  def handleCreateRollCall(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleCreateRollCall(rpcMessage)

  def handleOpenRollCall(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleOpenRollCall(rpcMessage)

  def handleReopenRollCall(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleReopenRollCall(rpcMessage)

  def handleCloseRollCall(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleCloseRollCall(rpcMessage)
}

/** Implementation of the RollCallHandler that provides a testable interface
  *
  * @param dbRef
  *   reference of the db actor
  */
class RollCallHandler(dbRef: => AskableActorRef) extends MessageHandler {

  /** Overrides default DbActor with provided parameter
    */
  override final val dbActor: AskableActorRef = dbRef

  private val serverUnexpectedAnswer: String = "The server is doing something unexpected"

  def handleCreateRollCall(rpcRequest: JsonRpcRequest): GraphMessage = {
    val ask =
      for {
        (_, message, somedata) <- extractParameters[CreateRollCall](rpcRequest, serverUnexpectedAnswer)
        data: CreateRollCall = somedata.get
        rollCallChannel: Channel = Channel(s"${Channel.ROOT_CHANNEL_PREFIX}${data.id}")
        laoId: Hash = rpcRequest.extractLaoId
        _ <- dbActor ? DbActor.AssertChannelMissing(rollCallChannel)
        // we create a new channel to write uniquely the RollCall, this ensures then if the RollCall already exists or not
        // otherwise, we never write in this channel
        _ <- dbActor ? DbActor.CreateChannel(rollCallChannel, ObjectType.ROLL_CALL)
        _ <- dbAskWritePropagate(rpcRequest)
        _ <- dbActor ? DbActor.WriteRollCallData(laoId, message)
      } yield ()

    Await.ready(ask, duration).value match {
      case Some(Success(_))                        => Right(rpcRequest)
      case Some(Failure(ex: DbActorNAckException)) => Left(PipelineError(ex.code, s"handleCreateRollCall failed : ${ex.message}", rpcRequest.getId))
      case reply                                   => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleCreateRollCall failed : unexpected DbActor reply '$reply'", rpcRequest.getId))
    }
  }

  def handleOpenRollCall(rpcRequest: JsonRpcRequest): GraphMessage = {
    val ask =
      for {
        (_, message, _) <- extractParameters[OpenRollCall](rpcRequest, serverUnexpectedAnswer) // if it fails, throw an error
        channel: Channel = rpcRequest.getParamsChannel
        laoId: Hash = rpcRequest.extractLaoId
        // check if the roll call already exists to open it
        _ <- dbActor ? DbActor.ChannelExists(channel)
        _ <- dbAskWritePropagate(rpcRequest)
        // creates a RollCallData
        _ <- dbActor ? DbActor.WriteRollCallData(laoId, message)
      } yield ()

    Await.ready(ask, duration).value match {
      case Some(Success(_))                        => Right(rpcRequest)
      case Some(Failure(ex: DbActorNAckException)) => Left(PipelineError(ex.code, s"handleOpenRollCall failed : ${ex.message}", rpcRequest.getId))
      case reply                                   => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleOpenRollCall failed : unexpected DbActor reply '$reply'", rpcRequest.getId))
    }
  }

  def handleReopenRollCall(rpcRequest: JsonRpcRequest): GraphMessage = {
    val ask = for {
      (_, message, _) <- extractParameters[ReopenRollCall](rpcRequest, serverUnexpectedAnswer)
      laoId: Hash = rpcRequest.extractLaoId
      _ <- dbAskWritePropagate(rpcRequest)
      _ <- dbActor ? DbActor.WriteRollCallData(laoId, message)
    } yield ()

    Await.ready(ask, duration).value match {
      case Some(Success(_))                        => Right(rpcRequest)
      case Some(Failure(ex: DbActorNAckException)) => Left(PipelineError(ex.code, s"handleReOpenRollCall failed : ${ex.message}", rpcRequest.getId))
      case reply                                   => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleRepenRollCall failed : unexpected DbActor reply '$reply'", rpcRequest.getId))
    }
  }

  def handleCloseRollCall(rpcRequest: JsonRpcRequest): GraphMessage = {
    val combined = for {
      (_, laoChannel) <- extractLaoChannel(rpcRequest, s"There is an issue with the data of the LAO")
      _ <- dbAskWritePropagate(rpcRequest)
      (_, message, _) <- extractParameters[CloseRollCall](rpcRequest, serverUnexpectedAnswer)
      _ <- dbActor ? DbActor.WriteLaoData(rpcRequest.getParamsChannel, message, None)
      _ <- dbActor ? DbActor.WriteRollCallData(laoChannel.get, message)
    } yield ()

    Await.ready(combined, duration).value match {
      case Some(Success(_))                        => createAttendeeChannels(rpcRequest)
      case Some(Failure(ex: DbActorNAckException)) => Left(PipelineError(ex.code, s"handleCloseRollCall failed : ${ex.message}", rpcRequest.getId))
      case reply                                   => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleCloseRollCall failed : unexpected DbActor reply '$reply'", rpcRequest.getId))
    }
  }

  private def generateSocialChannel(channel: Channel, pk: PublicKey): Channel = Channel(s"$channel${Channel.SOCIAL_CHANNEL_PREFIX}${pk.base64Data}")

  // creates a channel for each attendee (of name /root/lao_id/social/PublicKeyAttendee), returns a GraphMessage
  private def createAttendeeChannels(rpcRequest: JsonRpcRequest): GraphMessage = {
    val message: Message = rpcRequest.getParamsMessage.get
    val data: CloseRollCall = message.decodedData.get.asInstanceOf[CloseRollCall]
    val listAttendeeChannels: List[(Channel, ObjectType.ObjectType)] = data.attendees.map {
      attendee => (generateSocialChannel(rpcRequest.getParamsChannel, attendee), ObjectType.CHIRP)
    }

    val askCreateChannels = dbActor ? DbActor.CreateChannelsFromList(listAttendeeChannels)

    Await.ready(askCreateChannels, duration).value match {
      case Some(Success(_))                        => Right(rpcRequest)
      case Some(Failure(ex: DbActorNAckException)) => Left(PipelineError(ex.code, s"handleCloseRollCall failed : ${ex.message}", rpcRequest.getId))
      case reply                                   => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleCloseRollCall failed : unexpected DbActor reply '$reply'", rpcRequest.getId))
    }
  }
}
