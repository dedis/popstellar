package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.rollCall.{CloseRollCall, CreateRollCall, OpenRollCall}
import ch.epfl.pop.model.objects.{Hash, Timestamp}
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}


case object RollCallValidator extends MessageDataContentValidator with EventValidator {
  override def EVENT_HASH_PREFIX: String = "R"

  override def generateValidationId(hash: Hash, timestamp: Timestamp, string: String): Hash =
    Hash.fromStrings(EVENT_HASH_PREFIX, hash.toString, timestamp.toString, string)

  def validateCreateRollCall(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "CreateRollCall", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: CreateRollCall = message.decodedData.get.asInstanceOf[CreateRollCall]

        val laoId: Hash = rpcMessage.extractLaoId
        val expectedRollCallId: Hash = generateValidationId(laoId, data.creation, data.name)

        if (!validateTimestampStaleness(data.creation)) {
          Right(validationError(s"stale 'creation' timestamp (${data.creation})"))
        } else if (!validateTimestampOrder(data.creation, data.proposed_start)) {
          Right(validationError(s"'proposed_start' (${data.proposed_start}) timestamp is younger than 'creation' (${data.creation})"))
        } else if (!validateTimestampOrder(data.proposed_start, data.proposed_end)) {
          Right(validationError(s"'proposed_end' (${data.proposed_end}) timestamp is younger than 'proposed_start' (${data.proposed_start})"))
        } else if (expectedRollCallId != data.id) {
          Right(validationError("unexpected id"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateOpenRollCall(rpcMessage: JsonRpcRequest, validatorName: String = "OpenRollCall"): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, validatorName, rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: OpenRollCall = message.decodedData.get.asInstanceOf[OpenRollCall]

        val laoId: Hash = rpcMessage.extractLaoId
        // Note: this id doesnt follow the standard sh256(string, hash, timestamp, string) other events do;
        // this is why generateValidationId cannot be used
        val expectedRollCallId: Hash = Hash.fromStrings(
          EVENT_HASH_PREFIX, laoId.toString, data.opens.toString, data.opened_at.toString
        )

        if (!validateTimestampStaleness(data.opened_at)) {
          Right(validationError(s"stale 'opened_at' timestamp (${data.opened_at})"))
        } else if (expectedRollCallId != data.opens) {
          Right(validationError("unexpected id 'opens'"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateReopenRollCall(rpcMessage: JsonRpcRequest): GraphMessage = {
    validateOpenRollCall(rpcMessage, "ReopenRollCall")
  }

  // TODO check that this is correct (correct validations)
  def validateCloseRollCall(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "CloseRollCall", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: CloseRollCall = message.decodedData.get.asInstanceOf[CloseRollCall]

        val laoId: Hash = rpcMessage.extractLaoId
        // Note: this id doesnt follow the standard sh256(string, hash, timestamp, string) other events do;
        // this is why generateValidationId cannot be used
        val expectedRollCallId: Hash = Hash.fromStrings(
          EVENT_HASH_PREFIX, laoId.toString, data.closes.toString, data.closed_at.toString
        )

        if (!validateTimestampStaleness(data.closed_at)) {
          Right(validationError(s"stale 'closed_at' timestamp (${data.closed_at})"))
        } else if (data.attendees.size != data.attendees.toSet.size) {
          Right(validationError("duplicate attendees keys"))
        } else if (expectedRollCallId != data.closes) {
          Right(validationError("unexpected id 'closes'"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }
}
