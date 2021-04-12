package ch.epfl.pop.pubsub.graph.validators
import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.lao.{CreateLao, StateLao, UpdateLao}
import ch.epfl.pop.model.network.requests.lao.{JsonRpcRequestCreateLao, JsonRpcRequestStateLao, JsonRpcRequestUpdateLao}
import ch.epfl.pop.model.objects.Hash
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}

case object LaoValidator extends ContentValidator {
  /**
   * Validates a GraphMessage containing a lao rpc-message or a pipeline error
   */
  override val validator: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Left(jsonRpcMessage) => jsonRpcMessage match {
      case message@(_: JsonRpcRequestCreateLao) => validateCreateLao(message)
      case message@(_: JsonRpcRequestStateLao) => validateStateLao(message)
      case message@(_: JsonRpcRequestUpdateLao) => validateUpdateLao(message)
      case _ => Right(PipelineError(
        ErrorCodes.SERVER_FAULT.id,
        "Internal server fault: LaoValidator was given a message it could not recognize"
      ))
    }
    case graphMessage@_ => graphMessage
  }

  sealed def validateCreateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "CreateLao")

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: CreateLao = message.decodedData.asInstanceOf[CreateLao]
        val expectedHash: Hash = Hash.fromStrings(data.organizer.toString, data.creation.toString, data.name)

        if (!validateTimestampStaleness(data.creation)) {
          Right(validationError(s"stale 'creation' timestamp (${data.creation})"))
        } else if (!validateWitnesses(data.witnesses)) {
          Right(validationError("duplicate witnesses keys"))
        } else if (expectedHash != data.id) {
          Right(validationError("unexpected id"))
        } else if (data.organizer != message.sender) {
          Right(validationError("unexpected organizer public key"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage)
    }
  }

  sealed def validateStateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "StateLao")

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: StateLao = message.decodedData.asInstanceOf[StateLao]
        val expectedHash: Hash = Hash.fromStrings(data.organizer.toString, data.creation.toString, data.name)

        if (!validateTimestampStaleness(data.creation)) {
          Right(validationError(s"stale 'creation' timestamp (${data.creation})"))
        } else if (!validateTimestampOrder(data.creation, data.last_modified)) {
          Right(validationError(s"'last_modified' (${data.last_modified}) timestamp is younger than 'creation' (${data.creation})"))
        } else if (!validateWitnesses(data.witnesses)) {
          Right(validationError("duplicate witnesses keys"))
        } else if (!validateWitnessSignatures(data.modification_signatures, data.modification_id)) {
          Right(validationError("witness key-signature pairs are not valid for the given modification_id"))
        } else if (expectedHash != data.id) {
          Right(validationError("unexpected id"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage)
    }
  }

  sealed def validateUpdateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "UpdateLao")

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: UpdateLao = message.decodedData.asInstanceOf[UpdateLao]
        val expectedHash: Hash = Hash.fromStrings() // FIXME get id from db

        if (!validateTimestampStaleness(data.last_modified)) {
          Right(validationError(s"stale 'last_modified' timestamp (${data.last_modified})"))
        } else if (!validateWitnesses(data.witnesses)) {
          Right(validationError("duplicate witnesses keys"))
        } else if (expectedHash != data.id) {
          Right(validationError("unexpected id"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage)
    }
  }
}
