package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.socialMedia._
import ch.epfl.pop.model.objects.{Channel, PublicKey}
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}

import MessageValidator._

import scala.concurrent.ExecutionContext.Implicits.global



case object SocialMediaValidator extends MessageDataContentValidator with EventValidator {

    override def EVENT_HASH_PREFIX: String = Channel.SEPARATOR + "posts"

    def validateAddChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
        def validationError(reason: String): PipelineError = super.validationError(reason, "AddChirp", rpcMessage.id)

        rpcMessage.getParamsMessage match {

            // TODO need more checks
            case Some(message) => {
                val data: AddChirp = message.decodedData.get.asInstanceOf[AddChirp]

                val sender: PublicKey = message.sender //sender's PK
                val channel: Channel = rpcMessage.getParamsChannel

                if (!validateTimestampStaleness(data.timestamp)) {
                    Right(validationError(s"stale timestamp (${data.timestamp})"))
                } else if (!validateChannelType(ObjectType.CHIRP, channel)){
                    Right(validationError(s"trying to add a Chirp on a wrong type of channel $channel"))
                } else if (!validateAttendee(sender, channel)){
                    Right(validationError(s"Sender $sender has an invalid PoP token."))
                } else if (channel.extractChildChannel.base64Data != sender.base64Data) {
                    Right(validationError(s"Sender $sender has an invalid PoP token - doesn't own the channel."))
                }
                // FIXME: validate parent ID: check with ChannelData object
                else{
                    Left(rpcMessage)
                }
            }
            case _ => Right(validationErrorNoMessage(rpcMessage.id))
        }
    }

    //no need for validation for now, as the server is not supposed to receive the broadcasts
    def validateNotifyAddChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
        Left(rpcMessage)
    }

    def validateDeleteChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
        def validationError(reason: String): PipelineError = super.validationError(reason, "DeleteChirp", rpcMessage.id)

        rpcMessage.getParamsMessage match {

            // TODO need more checks
            case Some(message) => {
                val data: DeleteChirp = message.decodedData.get.asInstanceOf[DeleteChirp]

                val sender: PublicKey = message.sender //sender's PK
                val channel: Channel = rpcMessage.getParamsChannel

                if (!validateTimestampStaleness(data.timestamp)) {
                    Right(validationError(s"stale timestamp (${data.timestamp})"))
                } else if (!validateChannelType(ObjectType.CHIRP, channel)){
                    Right(validationError(s"trying to add a Chirp on a wrong type of channel $channel"))
                } else if (!validateAttendee(sender, channel)){
                    Right(validationError(s"Sender $sender has an invalid PoP token."))
                } else if (channel.extractChildChannel.base64Data != sender.base64Data) {
                    Right(validationError(s"Sender $sender has an invalid PoP token - doesn't own the channel."))
                }
                // FIXME: validate parent ID: check with ChannelData object
                else{
                    Left(rpcMessage)
                }
            }
            case _ => Right(validationErrorNoMessage(rpcMessage.id))
        }
    }

    //no need for validation for now, as the server is not supposed to receive the broadcasts
    def validateNotifyDeleteChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
        Left(rpcMessage)
    }

    def validateAddReaction(rpcMessage: JsonRpcRequest): GraphMessage = {
        def validationError(reason: String): PipelineError = super.validationError(reason, "DeleteReaction", rpcMessage.id)

        rpcMessage.getParamsMessage match {

            case Some(message) => {
                val data: AddReaction = message.decodedData.get.asInstanceOf[AddReaction]
                val sender: PublicKey = message.sender //sender's PK
                val channel: Channel = rpcMessage.getParamsChannel

                if (!validateTimestampStaleness(data.timestamp)) {
                    Right(validationError(s"stale timestamp (${data.timestamp})"))
                } else if (!validateChannelType(ObjectType.REACTION, channel)){
                    Right(validationError(s"trying to add a Chirp on a wrong type of channel $channel"))
                } else if (!validateAttendee(sender, channel)){
                    Right(validationError(s"Sender $sender has an invalid PoP token."))
                } else{
                    Left(rpcMessage)
                }
            }
            case _ => Right(validationErrorNoMessage(rpcMessage.id))
        }
    }

    def validateDeleteReaction(rpcMessage: JsonRpcRequest): GraphMessage = {
        def validationError(reason: String): PipelineError = super.validationError(reason, "DeleteReaction", rpcMessage.id)

        rpcMessage.getParamsMessage match {

            case Some(message) => {
                val data: DeleteReaction = message.decodedData.get.asInstanceOf[DeleteReaction]
                val sender: PublicKey = message.sender //sender's PK
                val channel: Channel = rpcMessage.getParamsChannel

                if (!validateTimestampStaleness(data.timestamp)) {
                    Right(validationError(s"stale timestamp (${data.timestamp})"))
                } else if (!validateChannelType(ObjectType.REACTION, channel)){
                    Right(validationError(s"trying to add a Chirp on a wrong type of channel $channel"))
                } else if (!validateAttendee(sender, channel)){
                    Right(validationError(s"Sender $sender has an invalid PoP token."))
                } else{
                    Left(rpcMessage)
                }
            }
            case _ => Right(validationErrorNoMessage(rpcMessage.id))
        }
    }
}
