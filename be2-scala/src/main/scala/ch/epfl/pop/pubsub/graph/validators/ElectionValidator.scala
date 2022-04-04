package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.election.{CastVoteElection, EndElection, OpenElection, ResultElection, SetupElection}
import ch.epfl.pop.model.objects.{Base64Data, Channel, Hash, PublicKey}
import ch.epfl.pop.pubsub.graph.validators.MessageValidator._
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}

object ElectionValidator extends MessageDataContentValidator with EventValidator {
  override val EVENT_HASH_PREFIX: String = "Election"

  def validateSetupElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "SetupElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: SetupElection = message.decodedData.get.asInstanceOf[SetupElection]

        val laoId: Hash = rpcMessage.extractLaoId
        val expectedHash: Hash = Hash.fromStrings(EVENT_HASH_PREFIX, laoId.toString, data.created_at.toString, data.name)

        val sender: PublicKey = message.sender
        val channel: Channel = rpcMessage.getParamsChannel

        if (!validateTimestampStaleness(data.created_at)) {
          Right(validationError(s"stale 'created_at' timestamp (${data.created_at})"))
        } else if (!validateTimestampOrder(data.created_at, data.start_time)) {
          Right(validationError(s"'start_time' (${data.start_time}) timestamp is smaller than 'created_at' (${data.created_at})"))
        } else if (!validateTimestampOrder(data.start_time, data.end_time)) {
          Right(validationError(s"'end_time' (${data.end_time}) timestamp is smaller than 'start_time' (${data.start_time})"))
        } else if (expectedHash != data.id) {
          Right(validationError("unexpected id"))
        } else if (!validateOwner(sender, channel)) {
          Right(validationError(s"invalid sender $sender"))
        } //note: the SetupElection is the only message sent to the main channel, others are sent in an election channel
        else if (!validateChannelType(ObjectType.LAO, channel)) {
          Right(validationError(s"trying to send a SetupElection message on a wrong type of channel $channel"))
        } else {
          Left(rpcMessage)
        }

      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateOpenElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "OpenElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: OpenElection = message.decodedData.get.asInstanceOf[OpenElection]

        val electionId: Hash = rpcMessage.extractLaoId
        val sender: PublicKey = message.sender

        val channel: Channel = rpcMessage.getParamsChannel
        val laoId: Base64Data = channel.decodeChannelLaoId.get

        if (!validateTimestampStaleness(data.opened_at)) {
          Right(validationError(s"stale 'opened_at' timestamp (${data.opened_at})"))
        } else if (electionId !=  data.election) {
          Right(validationError("Unexpected election id"))
        } else if (laoId != data.lao.base64Data) {
          Right(validationError("Unexpected lao id"))
        } else if (!validateOwner(sender, channel)) {
          Right(validationError(s"Sender $sender has an invalid PoP token."))
        } else if (!validateChannelType(ObjectType.ELECTION, channel)) {
          Right(validationError(s"trying to send a OpenElection message on a wrong type of channel $channel"))
        } else {
          Left(rpcMessage)
        }

      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateCastVoteElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "CastVoteElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: CastVoteElection = message.decodedData.get.asInstanceOf[CastVoteElection]
        val electionId: Hash = rpcMessage.extractLaoId
        val sender: PublicKey = message.sender

        val channel: Channel = rpcMessage.getParamsChannel
        val laoId: Base64Data = channel.decodeChannelLaoId.get

        if (!validateTimestampStaleness(data.created_at)) {
          Right(validationError(s"stale 'created_at' timestamp (${data.created_at})"))
        } else if (electionId != data.election) {
          Right(validationError("unexpected election id"))
        } else if (laoId != data.lao.base64Data){
          Right(validationError("unexpected lao id"))
        } else if (!validateAttendee(sender, channel)) {
          Right(validationError(s"Sender $sender has an invalid PoP token."))
        } else if (!validateChannelType(ObjectType.ELECTION, channel)) {
          Right(validationError(s"trying to send a CastVoteElection message on a wrong type of channel $channel"))
        } else {
          Left(rpcMessage)
        }

      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateResultElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "ResultElection", rpcMessage.id)
    //TODO : need to check the hash id if they correspond to the registerd ones
    rpcMessage.getParamsMessage match {
      case Some(message) =>
        val data: ResultElection = message.decodedData.get.asInstanceOf[ResultElection]

        val sender: PublicKey = message.sender
        val channel: Channel = rpcMessage.getParamsChannel

        if (!validateOwner(sender, channel)) {
          Right(validationError(s"invalid sender $sender"))
        } else if (!validateChannelType(ObjectType.ELECTION, channel)) {
          Right(validationError(s"trying to send a ResultElection message on a wrong type of channel $channel"))
        } else {
          Left(rpcMessage)
        }
      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }

  def validateEndElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    def validationError(reason: String): PipelineError = super.validationError(reason, "EndElection", rpcMessage.id)

    rpcMessage.getParamsMessage match {
      case Some(message: Message) =>
        val data: EndElection = message.decodedData.get.asInstanceOf[EndElection]
        val electionId: Hash = rpcMessage.extractLaoId

        val sender: PublicKey = message.sender

        val channel: Channel = rpcMessage.getParamsChannel
        val laoId: Base64Data = channel.decodeChannelLaoId.get

        if (!validateTimestampStaleness(data.created_at)) {
          Right(validationError(s"stale 'created_at' timestamp (${data.created_at})"))
        } else if (electionId != data.election) {
          Right(validationError("unexpected election id"))
        } else if (laoId != data.lao.base64Data) {
          Right(validationError("unexpected lao id"))
        } else if (!validateOwner(sender, channel)) {
          Right(validationError(s"invalid sender $sender"))
        } else if (!validateChannelType(ObjectType.ELECTION, channel)) {
          Right(validationError(s"trying to send a EndElection message on a wrong type of channel $channel"))
        } else {
          Left(rpcMessage)
        }

      case _ => Right(validationErrorNoMessage(rpcMessage.id))
    }
  }
}
