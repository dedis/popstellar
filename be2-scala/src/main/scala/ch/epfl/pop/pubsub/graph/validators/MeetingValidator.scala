package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.meeting.{CreateMeeting, StateMeeting}
import ch.epfl.pop.model.objects.Hash
import ch.epfl.pop.pubsub.graph.validators.MessageValidator._
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}
import akka.pattern.AskableActorRef
import ch.epfl.pop.storage.DbActor

object MeetingValidator extends MessageDataContentValidator with EventValidator {

  val meetingValidator = new MeetingValidator(DbActor.getInstance)

  override val EVENT_HASH_PREFIX: String = meetingValidator.EVENT_HASH_PREFIX

  def validateCreateMeeting(rpcMessage: JsonRpcRequest): GraphMessage = meetingValidator.validateCreateMeeting(rpcMessage)

  def validateStateMeeting(rpcMessage: JsonRpcRequest): GraphMessage = meetingValidator.validateStateMeeting(rpcMessage)

}

sealed class MeetingValidator(dbActorRef: => AskableActorRef) extends MessageDataContentValidator with EventValidator {
  override val EVENT_HASH_PREFIX: String = "M"

  def validateCreateMeeting(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "CreateMeeting", rpcMessage.id)


    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val (data, laoId, sender, channel) = extractData[CreateMeeting](rpcMessage)
        val expectedHash: Hash = Hash.fromStrings(EVENT_HASH_PREFIX, laoId.toString, data.creation.toString, data.name)
        runChecks(
          checkTimestampStaleness(
            rpcMessage,
            data.creation,
            validationError(s"stale 'creation' timestamp (${data.creation})")
          ),
          checkTimestampStaleness(
            rpcMessage,
            data.start,
            validationError(s"stale 'start' timestamp (${data.start})")
          ),
          checkTimestampOrder(
            rpcMessage,
            data.creation,
            data.end.get,
            validationError(s"'end' (${data.end.get}) timestamp is smaller than 'creation' (${data.creation})")
          ),
          checkTimestampOrder(
            rpcMessage,
            data.start,
            data.end.get,
            validationError(s"'end' (${data.end.get}) timestamp is smaller than 'start' (${data.start})")
          ),
          checkId(rpcMessage, expectedHash, data.id, validationError(s"unexpected id")),
          checkOwner(rpcMessage, sender, channel, dbActorRef, validationError(s"invalid sender $sender")),
          checkChannelType(
            rpcMessage,
            ObjectType.LAO,
            channel,
            dbActorRef,
            validationError(s"trying to send an CreateMeeting message on a wrong type of channel $channel")
          )
        )
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateStateMeeting(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "StateMeeting", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val (data, laoId, _, _) = extractData[StateMeeting](rpcMessage)
        val expectedHash: Hash = Hash.fromStrings(EVENT_HASH_PREFIX, laoId.toString, data.creation.toString, data.name)

        runChecks(
          checkTimestampStaleness(
            rpcMessage,
            data.creation,
            validationError(s"stale 'creation' timestamp (${data.creation})")
          ),
          checkTimestampOrder(
            rpcMessage,
            data.creation,
            data.last_modified,
            validationError(s"'last_modified' (${data.last_modified}) timestamp is smaller than 'creation' (${data.creation})")
          ),
          checkTimestampStaleness(
            rpcMessage,
            data.start,
            validationError(s"stale 'start' timestamp (${data.start})")
          ),
          checkOptionalTimestampOrder(
            rpcMessage,
            data.creation,
            data.end,
            validationError(s"'end' (${data.end.get}) timestamp is smaller than 'creation' (${data.creation})")
          ),
          checkOptionalTimestampOrder(
            rpcMessage,
            data.start,
            data.end,
            validationError(s"'end' (${data.end.get}) timestamp is smaller than 'start' (${data.start})")
          ),
          checkId(rpcMessage, expectedHash, data.id, validationError(s"unexpected id")),
          checkWitnessesSignatures(
            rpcMessage,
            data.modification_signatures,
            data.modification_id,
            validationError("witness key-signature pairs are not valid for the given modification_id")
          )
        )
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

}
