package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.lao.{CreateLao, GreetLao, StateLao, UpdateLao}
import ch.epfl.pop.model.objects.{Channel, DbActorNAckException, Hash}
import ch.epfl.pop.pubsub.graph.validators.MessageValidator._
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

import scala.concurrent._
import scala.util.{Failure, Success}

case object LaoValidator extends MessageDataContentValidator {
  def validateCreateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "CreateLao", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val Some(data: CreateLao) = message.decodedData
        val expectedHash: Hash = Hash.fromStrings(data.organizer.base64Data.toString, data.creation.toString, data.name)

        val channel: Channel = rpcMessage.getParamsChannel

        if (!validateTimestampStaleness(data.creation)) {
          Right(validationError(s"stale 'creation' timestamp (${data.creation})"))
        } else if (!validateWitnesses(data.witnesses)) {
          Right(validationError("duplicate witnesses keys"))
        } else if (expectedHash != data.id) {
          Right(validationError("unexpected id"))
        } else if (data.organizer != message.sender) {
          Right(validationError("unexpected organizer public key"))
        } else if (channel != Channel.ROOT_CHANNEL) {
          Right(validationError(s"trying to send a CreateLao message on a channel $channel other than ${Channel.ROOT_CHANNEL}"))
        } else if (data.name == "") {
          Right(validationError(s"LAO name must not be empty"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateStateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "StateLao", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val Some(data: StateLao) = message.decodedData

        val expectedHash: Hash = Hash.fromStrings(data.organizer.toString, data.creation.toString, data.name)

        if (!validateTimestampStaleness(data.creation)) {
          Right(validationError(s"stale 'creation' timestamp (${data.creation})"))
        } else if (!validateTimestampOrder(data.creation, data.last_modified)) {
          Right(validationError(s"'last_modified' (${data.last_modified}) timestamp is smaller than 'creation' (${data.creation})"))
        } else if (!validateWitnesses(data.witnesses)) {
          Right(validationError("duplicate witnesses keys"))
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

  def validateGreetLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "GreetLao", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val Some(data: GreetLao) = message.decodedData

        val channel: Channel = rpcMessage.getParamsChannel

        val expectedLaoId: Hash = rpcMessage.extractLaoId

        if (expectedLaoId != data.lao) {
          Right(validationError("unexpected id, was " + data.lao + " but expected " + expectedLaoId))
        } else if (data.frontend != message.sender) {
          Right(validationError("unexpected frontend"))
        } else if (!data.address.startsWith("ws://")) {
          Right(validationError("invalid address"))
        } else if (channel != Channel(s"${Channel.ROOT_CHANNEL_PREFIX}${data.lao}")) {
          Right(validationError(s"trying to write an GreetLao message on wrong channel $channel"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateUpdateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "UpdateLao", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val Some(data: UpdateLao) = message.decodedData

        val channel: Channel = rpcMessage.getParamsChannel

        // FIXME get lao creation message in order to calculate "SHA256(organizer||creation||name)"
        val askLaoMessage = dbActor ? DbActor.Read(rpcMessage.getParamsChannel, ???)

        Await.ready(askLaoMessage, duration).value match {
          case Some(Success(DbActor.DbActorReadAck(None))) =>
            Right(PipelineError(ErrorCodes.INVALID_RESOURCE.id, "validateUpdateLao failed : no CreateLao message associated found", rpcMessage.id))
          case Some(Success(DbActor.DbActorReadAck(Some(retrievedMessage)))) =>
            val Some(laoCreationMessage: CreateLao) = retrievedMessage.decodedData
            // Calculate expected hash
            val expectedHash: Hash = Hash.fromStrings(
              retrievedMessage.sender.toString,
              laoCreationMessage.creation.toString,
              laoCreationMessage.name
            )

            if (!validateTimestampStaleness(data.last_modified)) {
              Right(validationError(s"stale 'last_modified' timestamp (${data.last_modified})"))
            } else if (!validateWitnesses(data.witnesses)) {
              Right(validationError("duplicate witnesses keys"))
            } else if (expectedHash != data.id) {
              Right(validationError("unexpected id"))
            } else if (!validateChannelType(ObjectType.LAO, channel)) {
              Right(validationError(s"trying to write an UpdateLao message on wrong type of channel $channel"))
            } else {
              Left(rpcMessage)
            }
          case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"validateUpdateLao failed : ${ex.message}", rpcMessage.getId))
          case reply                                   => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"validateUpdateLao failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
        }

      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }
}
