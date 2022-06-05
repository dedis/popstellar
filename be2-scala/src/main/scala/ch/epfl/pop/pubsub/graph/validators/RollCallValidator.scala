package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.rollCall.{CloseRollCall, CreateRollCall, IOpenRollCall}
import ch.epfl.pop.model.objects.{Channel, Hash, PublicKey}
import ch.epfl.pop.pubsub.graph.validators.MessageValidator._
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor


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

  //remark: in all the validation functions, the channel type is ObjectType.LAO, which is the default ObjectType for all other messages apart from social media and elections
  def validateCreateRollCall(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "CreateRollCall", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: CreateRollCall = message.decodedData.get.asInstanceOf[CreateRollCall]

        val laoId: Hash = rpcMessage.extractLaoId
        val expectedRollCallId: Hash = Hash.fromStrings(EVENT_HASH_PREFIX, laoId.toString, data.creation.toString, data.name)

        val sender: PublicKey = message.sender
        val channel: Channel = rpcMessage.getParamsChannel

        if (!validateTimestampStaleness(data.creation)) {
          Right(validationError(s"stale 'creation' timestamp (${data.creation})"))
        } else if (!validateTimestampOrder(data.creation, data.proposed_start)) {
          Right(validationError(s"'proposed_start' (${data.proposed_start}) timestamp is smaller than 'creation' (${data.creation})"))
        } else if (!validateTimestampOrder(data.proposed_start, data.proposed_end)) {
          Right(validationError(s"'proposed_end' (${data.proposed_end}) timestamp is smaller than 'proposed_start' (${data.proposed_start})"))
        } else if (expectedRollCallId != data.id) {
          Right(validationError(s"unexpected id"))
        } else if (!validateOwner(sender, channel, dbActorRef)) {
          Right(validationError(s"invalid sender $sender"))
        } else if (!validateChannelType(ObjectType.LAO, channel, dbActorRef)) {
          Right(validationError(s"trying to send a CreateRollCall message on a wrong type of channel $channel"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  /**
   * Validates an rpcMessage for OpenRollCall message
   *
   * @param rpcMessage rpc message to validate
   * @return GraphMessage: passes the rpcMessages to Left if successful
   *         right with pipeline error
   */
  def validateOpenRollCall(rpcMessage: JsonRpcRequest, validatorName: String = "OpenRollCall"): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, validatorName, rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: IOpenRollCall = message.decodedData.get.asInstanceOf[IOpenRollCall]
        val laoId: Hash = rpcMessage.extractLaoId
        val expectedRollCallId: Hash = Hash.fromStrings(
          EVENT_HASH_PREFIX, laoId.toString, data.opens.toString, data.opened_at.toString
        )

        val sender: PublicKey = message.sender
        val channel: Channel = rpcMessage.getParamsChannel

        if (!validateTimestampStaleness(data.opened_at)) {
          Right(validationError(s"stale 'opened_at' timestamp (${data.opened_at})"))
        } else if (expectedRollCallId != data.update_id) {
          Right(validationError("unexpected id 'update_id'"))
        } else if (!validateOwner(sender, channel, dbActorRef)) {
          Right(validationError(s"invalid sender $sender"))
        } else if (!validateChannelType(ObjectType.LAO, channel, dbActorRef)) {
          Right(validationError(s"trying to send a $validatorName message on a wrong type of channel $channel"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  /**
   * Validates the rpcMessage for a ReOpenRollCall
   * similar to [[validateOpenRollCall]]
   *
   * @param rpcMessage rpc message to validate
   * @return GraphMessage: passes the rpcMessages to Left if successful
   *         right with pipeline error
   */
  def validateReopenRollCall(rpcMessage: JsonRpcRequest): GraphMessage = {
    validateOpenRollCall(rpcMessage, "ReopenRollCall")
  }

  def validateCloseRollCall(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "CloseRollCall", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: CloseRollCall = message.decodedData.get.asInstanceOf[CloseRollCall]

        val laoId: Hash = rpcMessage.extractLaoId
        val expectedRollCallId: Hash = Hash.fromStrings(
          EVENT_HASH_PREFIX, laoId.toString, data.closes.toString, data.closed_at.toString
        )

        val sender: PublicKey = message.sender
        val channel: Channel = rpcMessage.getParamsChannel

        if (!validateTimestampStaleness(data.closed_at)) {
          Right(validationError(s"stale 'closed_at' timestamp (${data.closed_at})"))
        } else if (data.attendees.size != data.attendees.toSet.size) {
          Right(validationError("duplicate attendees keys"))
        } else if (!validateAttendee(sender, channel, dbActorRef)) {
          Right(validationError("unexpected attendees keys"))
        } else if (expectedRollCallId != data.update_id) {
          Right(validationError("unexpected id 'update_id'"))
        } else if (data.update_id == data.closes) {
          Right(validationError("a closed roll call cannot be closed once again"))
        } else if (!validateOwner(sender, channel, dbActorRef)) {
          Right(validationError(s"invalid sender $sender"))
        } else if (!validateChannelType(ObjectType.LAO, channel, dbActorRef)) {
          Right(validationError(s"trying to send a CloseRollCall message on a wrong type of channel $channel"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }
}
