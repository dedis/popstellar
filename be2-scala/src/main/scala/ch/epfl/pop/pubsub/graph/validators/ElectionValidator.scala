package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.election.{EndElection, SetupElection}
import ch.epfl.pop.model.objects.Hash
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}

object ElectionValidator extends MessageDataContentValidator {
  def validateSetupElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "SetupElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: SetupElection = message.decodedData.get.asInstanceOf[SetupElection]

        val laoId: Hash = rpcMessage.extractLaoId
        val expectedHash: Hash = Hash.fromStrings("Election", laoId.toString, data.created_at.toString, data.name)

        if (!validateTimestampStaleness(data.created_at)) {
          Right(validationError(s"stale 'created_at' timestamp (${data.created_at})"))
        } else if (!validateTimestampOrder(data.created_at, data.start_time)) {
          Right(validationError(s"'start_time' (${data.start_time}) timestamp is younger than 'created_at' (${data.created_at})"))
        } else if (!validateTimestampOrder(data.start_time, data.end_time)) {
          Right(validationError(s"'end_time' (${data.end_time}) timestamp is younger than 'start_time' (${data.start_time})"))
        } else if (expectedHash != data.id) {
          Right(validationError("unexpected id"))
        } else {
          Left(rpcMessage)
        }

      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateResultElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "ResultElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(_) => Left(rpcMessage)
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateEndElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "EndElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: EndElection = message.decodedData.get.asInstanceOf[EndElection]

        val laoId: Hash = rpcMessage.extractLaoId
        val expectedElectionHash: Hash = Hash.fromStrings() // TODO calculate expected election hash

        if (!validateTimestampStaleness(data.created_at)) {
          Right(validationError(s"stale 'created_at' timestamp (${data.created_at})"))
        } else if (laoId != data.lao) {
          Right(validationError("unexpected lao id"))
        } else if (expectedElectionHash != data.election) {
          Right(validationError("unexpected election id"))
        } else {
          Left(rpcMessage)
        }

      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }
}
