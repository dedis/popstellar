package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.rollCall.CloseRollCall
import ch.epfl.pop.model.network.method.message.data.rollCall.CreateRollCall
import ch.epfl.pop.model.network.method.message.data.rollCall.IOpenRollCall
import ch.epfl.pop.model.network.method.message.data.rollCall.OpenRollCall
import ch.epfl.pop.model.network.method.message.data.rollCall.ReopenRollCall
import ch.epfl.pop.model.objects.Hash
import ch.epfl.pop.pubsub.graph.GraphMessage
import ch.epfl.pop.pubsub.graph.PipelineError


case object RollCallValidator extends MessageDataContentValidator with EventValidator {
  override def EVENT_HASH_PREFIX: String = "R"

  def validateCreateRollCall(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "CreateRollCall", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: CreateRollCall = message.decodedData.get.asInstanceOf[CreateRollCall]

        val laoId: Hash = rpcMessage.extractLaoId
        val expectedRollCallId: Hash = Hash.fromStrings(EVENT_HASH_PREFIX, laoId.toString, data.creation.toString, data.name)

        if (!validateTimestampStaleness(data.creation)) {
          Right(validationError(s"stale 'creation' timestamp (${data.creation})"))
        } else if (!validateTimestampOrder(data.creation, data.proposed_start)) {
          Right(validationError(s"'proposed_start' (${data.proposed_start}) timestamp is smaller than 'creation' (${data.creation})"))
        } else if (!validateTimestampOrder(data.proposed_start, data.proposed_end)) {
          Right(validationError(s"'proposed_end' (${data.proposed_end}) timestamp is smaller than 'proposed_start' (${data.proposed_start})"))
        } else if (expectedRollCallId != data.id) {
          Right(validationError("unexpected id"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  /**
   * Validates an rpcMessage for OpenRollCall message
   * @param rpcMessage
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

        if (!validateTimestampStaleness(data.opened_at)) {
          Right(validationError(s"stale 'opened_at' timestamp (${data.opened_at})"))
        } else if (expectedRollCallId != data.update_id) {
          Right(validationError("unexpected id 'update_id'"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  /**
    * Validates the rpcMessage for a ReOpenRollCall
    * similar to [[validateOpenRollCall]]
    * @param rpcMessage
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

        if (!validateTimestampStaleness(data.closed_at)) {
          Right(validationError(s"stale 'closed_at' timestamp (${data.closed_at})"))
        } else if (data.attendees.size != data.attendees.toSet.size) {
          Right(validationError("duplicate attendees keys"))
        } else if (expectedRollCallId != data.update_id) {
          Right(validationError("unexpected id 'update_id'"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }
}
