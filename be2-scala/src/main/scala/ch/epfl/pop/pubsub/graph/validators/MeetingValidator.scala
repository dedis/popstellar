package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.meeting.{CreateMeeting, StateMeeting}
import ch.epfl.pop.model.objects.{Hash, Timestamp}
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}


case object MeetingValidator extends MessageDataContentValidator with EventValidator {
  override def EVENT_HASH_PREFIX: String = "M"

  override def generateValidationId(hash: Hash, timestamp: Timestamp, string: String): Hash =
    Hash.fromStrings(EVENT_HASH_PREFIX, hash.toString, timestamp.toString, string)

  def validateCreateMeeting(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "CreateMeeting", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: CreateMeeting = message.decodedData.get.asInstanceOf[CreateMeeting]

        val laoId: Hash = rpcMessage.extractLaoId
        val expectedHash: Hash = generateValidationId(laoId, data.creation, data.name)

        if (!validateTimestampStaleness(data.creation)) {
          Right(validationError(s"stale 'creation' timestamp (${data.creation})"))
        } else if (!validateTimestampStaleness(data.start)) {
          Right(validationError(s"stale 'start' timestamp (${data.start})"))
        } else if (data.end.isDefined && !validateTimestampOrder(data.creation, data.end.get)) {
          Right(validationError(s"'end' (${data.end.get}) timestamp is smaller than 'creation' (${data.creation})"))
        } else if (expectedHash != data.id) {
          Right(validationError("unexpected id"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateStateMeeting(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "StateMeeting", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: StateMeeting = message.decodedData.get.asInstanceOf[StateMeeting]

        val laoId: Hash = rpcMessage.extractLaoId
        val expectedHash: Hash = generateValidationId(laoId, data.creation, data.name)

        if (!validateTimestampStaleness(data.creation)) {
          Right(validationError(s"stale 'creation' timestamp (${data.creation})"))
        } else if (!validateTimestampOrder(data.creation, data.last_modified)) {
          Right(validationError(s"'last_modified' (${data.last_modified}) timestamp is smaller than 'creation' (${data.creation})"))
        } else if (!validateTimestampStaleness(data.start)) {
          Right(validationError(s"stale 'start' timestamp (${data.start})"))
        } else if (data.end.isDefined && !validateTimestampOrder(data.creation, data.end.get)) {
          Right(validationError(s"'end' (${data.end.get}) timestamp is smaller than 'creation' (${data.creation})"))
        } else if (data.end.isDefined && !validateTimestampOrder(data.start, data.end.get)) {
          Right(validationError(s"'end' (${data.end.get}) timestamp is smaller than 'start' (${data.start})"))
        } else if (!validateWitnessSignatures(data.modification_signatures, data.modification_id)) {
          Right(validationError("witness key-signature pairs are not valid for the given modification_id"))
        } else if (expectedHash != data.id) {
          Right(validationError("unexpected id"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }
}
