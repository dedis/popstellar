package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.{JsonRpcRequest}
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ActionType.{CLOSE, CREATE, OPEN, REOPEN}
import ch.epfl.pop.model.network.method.message.data.{ObjectType}
import ch.epfl.pop.model.network.method.message.data.rollCall.{CloseRollCall, CreateRollCall, IOpenRollCall}
import ch.epfl.pop.model.objects.{Hash, RollCallData}
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
        val (data, laoId, sender, channel) = extractData[CreateRollCall](rpcMessage)
        val expectedRollCallId: Hash = Hash.fromStrings(
          EVENT_HASH_PREFIX,
          laoId.toString,
          data.creation.toString,
          data.name
        )

        runChecks(
          checkTimestampStaleness(
            rpcMessage,
            data.creation,
            validationError(s"stale 'creation' timestamp (${data.creation})")
          ),
          checkTimestampOrder(
            rpcMessage,
            data.creation,
            data.proposed_start,
            validationError(
              s"'proposed_start' (${data.proposed_start}) timestamp is smaller than 'creation' (${data.creation})"
            )
          ),
          checkTimestampOrder(
            rpcMessage,
            data.proposed_start,
            data.proposed_end,
            validationError(
              s"'proposed_end' (${data.proposed_end}) timestamp is smaller than 'proposed_start' (${data.proposed_start})"
            )
          ),
          checkId(rpcMessage, expectedRollCallId, data.id, validationError(s"unexpected id")),
          checkOwner(rpcMessage, sender, channel, dbActorRef, validationError(s"invalid sender $sender")),
          checkChannelType(
            rpcMessage,
            ObjectType.LAO,
            channel,
            dbActorRef,
            validationError(s"trying to send a CreateRollCall message on a wrong type of channel $channel")
          )
        )
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
        val (data, laoId, sender, channel) = extractData[IOpenRollCall](rpcMessage)
        val expectedRollCallId: Hash = Hash.fromStrings(
          EVENT_HASH_PREFIX,
          laoId.toString,
          data.opens.toString,
          data.opened_at.toString
        )

        runChecks(
          checkTimestampStaleness(
            rpcMessage,
            data.opened_at,
            validationError(s"stale 'opened_at' timestamp (${data.opened_at})")
          ),
          checkId(rpcMessage, expectedRollCallId, data.update_id, validationError("unexpected id 'update_id'")),
          checkOwner(rpcMessage, sender, channel, dbActorRef, validationError(s"invalid sender $sender")),
          checkChannelType(
            rpcMessage,
            ObjectType.LAO,
            channel,
            dbActorRef,
            validationError(s"trying to send a $validatorName message on a wrong type of channel $channel")
          ),
          validateOpens(rpcMessage, laoId, data.opens, validationError("unexpected id 'opens'"))
        )
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  /** Validates the opens id of a OpenRollCAll message
    *
    * @param rpcMessage
    *   rpc message to validate
    * @param laoId
    *   id of the LAO to which the rollCallDara belongs
    * @param opens
    *   opens id of the OpenRollCall which needs to be checked
    * @param error
    *   the error to forward in case the opens id does not correspond to the expected id
    * @return
    *   GraphMessage: passes the rpcMessages to Left if successful right with pipeline error
    */
  private def validateOpens(rpcMessage: JsonRpcRequest, laoId: Hash, opens: Hash, error: PipelineError): GraphMessage = {
    val rollCallData: Option[RollCallData] = getRollCallData(laoId)
    rollCallData match {
      case Some(data) =>
        if ((data.state == CREATE || data.state == CLOSE) && data.updateId == opens)
          Left(rpcMessage)
        else
          Right(error)
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
        val (data, laoId, sender, channel) = extractData[CloseRollCall](rpcMessage)
        val expectedRollCallId: Hash = Hash.fromStrings(
          EVENT_HASH_PREFIX,
          laoId.toString,
          data.closes.toString,
          data.closed_at.toString
        )

        runChecks(
          checkTimestampStaleness(
            rpcMessage,
            data.closed_at,
            validationError(s"stale 'closed_at' timestamp (${data.closed_at})")
          ),
          checkAttendeeSize(
            rpcMessage,
            data.attendees.size,
            data.attendees.toSet.size,
            validationError("duplicate attendees keys")
          ),
          checkAttendee(rpcMessage, sender, channel, dbActorRef, validationError("unexpected attendees keys")),
          checkId(rpcMessage, expectedRollCallId, data.update_id, validationError("unexpected id 'update_id'")),
          validateCloses(rpcMessage, laoId, data.closes, validationError("unexpected id 'closes'")),
          checkOwner(rpcMessage, sender, channel, dbActorRef, validationError(s"invalid sender $sender")),
          checkChannelType(
            rpcMessage,
            ObjectType.LAO,
            channel,
            dbActorRef,
            validationError(s"trying to send a CloseRollCall message on a wrong type of channel $channel")
          )
        )
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  /** Validates the closes id of CloseRollCall message
    *
    * @param rpcMessage
    *   rpc message to validate
    * @param laoId
    *   id of the LAO to which the rollCallDara belongs
    * @param closes
    *   closes id of CloseRollCall message which needs to be checked
    * @param error
    *   the error to forward in case the closes id does not correspond to the expected id
    * @return
    *   GraphMessage: passes the rpcMessages to Left if successful right with pipeline error
    */
  private def validateCloses(rpcMessage: JsonRpcRequest, laoId: Hash, closes: Hash, error: PipelineError): GraphMessage = {
    val rollCallData: Option[RollCallData] = getRollCallData(laoId)
    rollCallData match {
      case Some(data) =>
        if ((data.state == OPEN || data.state == REOPEN) && data.updateId == closes)
          Left(rpcMessage)
        else
          Right(error)
      case _ => Right(error)
    }
  }

  /** Checks if the number of attendees is as expected
    *
    * @param rpcMessage
    *   rpc message to validate
    * @param size
    *   size of the actual list of attendees
    * @param expectedSize
    *   expected size of attendees
    * @param error
    *   the error to forward in case the size is not as expected
    * @return
    *   GraphMessage: passes the rpcMessages to Left if successful right with pipeline error
    */
  private def checkAttendeeSize(rpcMessage: JsonRpcRequest, size: Int, expectedSize: Int, error: PipelineError): GraphMessage = {
    if (size == expectedSize)
      Left(rpcMessage)
    else
      Right(error)
  }
}
