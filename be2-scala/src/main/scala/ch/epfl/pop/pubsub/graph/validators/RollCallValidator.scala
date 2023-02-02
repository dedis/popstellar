package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.{JsonRpcMessage, JsonRpcRequest}
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ActionType.{ActionType, CLOSE, CREATE, OPEN, REOPEN}
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import ch.epfl.pop.model.network.method.message.data.rollCall.{CloseRollCall, CreateRollCall, IOpenRollCall}
import ch.epfl.pop.model.objects.{Channel, Hash, PublicKey, RollCallData}
import ch.epfl.pop.pubsub.graph.validators.MessageValidator._
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.DbActorReadRollCallDataAck

import scala.concurrent.Await
import scala.util.Success

object RollCallValidator extends MessageDataContentValidator with EventValidator {

  val rollCallValidator = new RollCallValidator(DbActor.getInstance)

  override val EVENT_HASH_PREFIX: String = rollCallValidator.EVENT_HASH_PREFIX

  def validateCreateRollCall(rpcMessage: JsonRpcRequest): GraphMessage = rollCallValidator.validateCreateRollCall(rpcMessage)

  def validateOpenRollCall(rpcMessage: JsonRpcRequest): GraphMessage = rollCallValidator.validateOpenRollCall(rpcMessage)

  def validateReopenRollCall(rpcMessage: JsonRpcRequest): GraphMessage = rollCallValidator.validateReopenRollCall(rpcMessage)

  def validateCloseRollCall(rpcMessage: JsonRpcRequest): GraphMessage = rollCallValidator.validateCloseRollCall(rpcMessage)

}

sealed class RollCallValidator(dbActorRef: => AskableActorRef) extends MessageDataContentValidator with EventValidator {

  override val EVENT_HASH_PREFIX: String = "R"

  private def runList(list: List[GraphMessage]): GraphMessage = {
    if (list.head.isLeft && !list.tail.isEmpty)
      runList(list.tail)
    else
      list.head
  }

  private def extractParameters[T](rpcMessage: JsonRpcRequest): (T, Hash, PublicKey, Channel) = {
    val message: Message = rpcMessage.getParamsMessage.get
    val data: T = message.decodedData.get.asInstanceOf[T]
    val laoId: Hash = rpcMessage.extractLaoId
    val sender: PublicKey = message.sender
    val channel: Channel = rpcMessage.getParamsChannel

    (data, laoId, sender, channel)
  }

  /** @param laoId
    *   LAO id of the channel
    * @return
    *   Rollcall Data of the channel
    */
  private def getRollCallData(laoId: Hash): Option[RollCallData] = {
    val ask = dbActorRef ? DbActor.ReadRollCallData(laoId)
    Await.ready(ask, duration).value match {
      case Some(Success(DbActorReadRollCallDataAck(rollcallData))) => Some(rollcallData)
      case _                                                       => None
    }
  }

  // remark: in all the validation functions, the channel type is ObjectType.LAO, which is the default ObjectType for all other messages apart from social media and elections
  def validateCreateRollCall(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "CreateRollCall", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val (data, laoId, sender, channel) = extractParameters[CreateRollCall](rpcMessage)
        val expectedRollCallId: Hash = Hash.fromStrings(EVENT_HASH_PREFIX, laoId.toString, data.creation.toString, data.name)

        runList(List(
          checkTimestampStaleness(rpcMessage, data.creation, validationError(s"stale 'creation' timestamp (${data.creation})")),
          checkTimestampOrder(
            rpcMessage,
            data.creation,
            data.proposed_start,
            validationError(s"'proposed_start' (${data.proposed_start}) timestamp is smaller than 'creation' (${data.creation})")
          ),
          checkTimestampOrder(
            rpcMessage,
            data.proposed_start,
            data.proposed_end,
            validationError(s"'proposed_end' (${data.proposed_end}) timestamp is smaller than 'proposed_start' (${data.proposed_start})")
          ),
          checkId(rpcMessage, expectedRollCallId, data.id, validationError(s"unexpected id")),
          checkOwner(rpcMessage, sender, channel, dbActorRef, validationError(s"invalid sender $sender")),
          checkChannelType(rpcMessage, ObjectType.LAO, channel, dbActorRef, validationError(s"trying to send a CreateRollCall message on a wrong type of channel $channel"))
        ))
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  /** Validates an rpcMessage for OpenRollCall message
    *
    * @param rpcMessage
    *   rpc message to validate
    * @return
    *   GraphMessage: passes the rpcMessages to Left if successful right with pipeline error
    */
  def validateOpenRollCall(rpcMessage: JsonRpcRequest, validatorName: String = "OpenRollCall"): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, validatorName, rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val (data, laoId, sender, channel) = extractParameters[IOpenRollCall](rpcMessage)
        val expectedRollCallId: Hash = Hash.fromStrings(
          EVENT_HASH_PREFIX,
          laoId.toString,
          data.opens.toString,
          data.opened_at.toString
        )

        runList(List(
          checkTimestampStaleness(rpcMessage, data.opened_at, validationError(s"stale 'opened_at' timestamp (${data.opened_at})")),
          checkId(rpcMessage, expectedRollCallId, data.update_id, validationError("unexpected id 'update_id'")),
          checkOwner(rpcMessage, sender, channel, dbActorRef, validationError(s"invalid sender $sender")),
          checkChannelType(rpcMessage, ObjectType.LAO, channel, dbActorRef, validationError(s"trying to send a $validatorName message on a wrong type of channel $channel")),
          validateOpens(rpcMessage, laoId, data.opens, validationError("unexpected id 'opens'"))
        ))
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  private def validateOpens(rpcMessage: JsonRpcRequest, laoId: Hash, opens: Hash, error: PipelineError): GraphMessage = {
    val rollCallData: Option[RollCallData] = getRollCallData(laoId)
    rollCallData match {
      case Some(data) =>
        if ((data.state == CREATE || data.state == CLOSE) && data.updateId == opens) Left(rpcMessage) else Right(error)
      case _ => Right(error)
    }
  }

  /** Validates the rpcMessage for a ReOpenRollCall similar to [[validateOpenRollCall]]
    *
    * @param rpcMessage
    *   rpc message to validate
    * @return
    *   GraphMessage: passes the rpcMessages to Left if successful right with pipeline error
    */
  def validateReopenRollCall(rpcMessage: JsonRpcRequest): GraphMessage = {
    validateOpenRollCall(rpcMessage, "ReopenRollCall")
  }

  def validateCloseRollCall(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "CloseRollCall", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val (data, laoId, sender, channel) = extractParameters[CloseRollCall](rpcMessage)
        val expectedRollCallId: Hash = Hash.fromStrings(
          EVENT_HASH_PREFIX,
          laoId.toString,
          data.closes.toString,
          data.closed_at.toString
        )

        runList(List(
          checkTimestampStaleness(rpcMessage, data.closed_at, validationError(s"stale 'closed_at' timestamp (${data.closed_at})")),
          checkAttendeeSize(rpcMessage, data.attendees.size, data.attendees.toSet.size, validationError("duplicate attendees keys")),
          checkAttendee(rpcMessage, sender, channel, dbActorRef, validationError("unexpected attendees keys")),
          checkId(rpcMessage, expectedRollCallId, data.update_id, validationError("unexpected id 'update_id'")),
          validateCloses(rpcMessage, laoId, data.closes, validationError("unexpected id 'closes'")),
          checkOwner(rpcMessage, sender, channel, dbActorRef, validationError(s"invalid sender $sender")),
          checkChannelType(rpcMessage, ObjectType.LAO, channel, dbActorRef, validationError(s"trying to send a CloseRollCall message on a wrong type of channel $channel"))
        ))
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  private def validateCloses(rpcMessage: JsonRpcRequest, laoId: Hash, closes: Hash, error: PipelineError): GraphMessage = {
    val rollCallData: Option[RollCallData] = getRollCallData(laoId)
    rollCallData match {
      case Some(data) => if ((data.state == OPEN || data.state == REOPEN) && data.updateId == closes) Left(rpcMessage) else Right(error)
      case _          => Right(error)
    }
  }

  private def checkAttendeeSize(rpcMessage: JsonRpcRequest, size: Int, expectedSize: Int, error: PipelineError): GraphMessage = {
    if (size == expectedSize) Left(rpcMessage) else Right(error)
  }
}
