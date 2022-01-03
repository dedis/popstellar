package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.socialMedia.AddChirp
import ch.epfl.pop.model.objects.{Channel, PublicKey}
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}

import MessageValidator._

import scala.concurrent.ExecutionContext.Implicits.global



case object SocialMediaValidator extends MessageDataContentValidator with EventValidator {

    override def EVENT_HASH_PREFIX: String = Channel.SEPARATOR + "posts"

    def validateAddChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
        def validationError(reason: String): PipelineError = super.validationError(reason, "AddChirp", rpcMessage.id)

        rpcMessage.getParamsMessage match {

            // FIXME need more checks
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

    
    def validateAddBroadcastChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
        Left(rpcMessage)
    }
}
