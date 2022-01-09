package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.socialMedia._
import ch.epfl.pop.model.objects.{Channel, PublicKey}
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}

import scala.concurrent.ExecutionContext.Implicits.global



case object SocialMediaValidator extends MessageDataContentValidator with EventValidator {

    override def EVENT_HASH_PREFIX: String = Channel.SEPARATOR + "posts"

    def validateAddChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
        def validationError(reason: String): PipelineError = super.validationError(reason, "AddChirp", rpcMessage.id)

        rpcMessage.getParamsMessage match {

            // FIXME need more checks
            case Some(message) => {
                val data: AddChirp = message.decodedData.get.asInstanceOf[AddChirp]

                val actualSender: PublicKey = message.sender //sender's PK

                if (!validateTimestampStaleness(data.timestamp)) {
                    Right(validationError(s"stale timestamp (${data.timestamp})"))
                }
                // FIXME: validate parent ID: check with ChannelData object
                else{
                    Left(rpcMessage)
                }
            }
            case _ => Right(validationErrorNoMessage(rpcMessage.id))
        }
    }

    
    def validateNotifyAddChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
        Left(rpcMessage)
    }

    def validateAddReaction(rpcMessage: JsonRpcRequest): GraphMessage = {
        def validationError(reason: String): PipelineError = super.validationError(reason, "DeleteReaction", rpcMessage.id)

        rpcMessage.getParamsMessage match {

            case Some(message) => {
                val data: AddReaction = message.decodedData.get.asInstanceOf[AddReaction]

                if (!validateTimestampStaleness(data.timestamp)) {
                    Right(validationError(s"stale timestamp (${data.timestamp})"))
                }
                // FIXME: add more checks
                else{
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

                if (!validateTimestampStaleness(data.timestamp)) {
                    Right(validationError(s"stale timestamp (${data.timestamp})"))
                }
                // FIXME: add more checks
                else{
                    Left(rpcMessage)
                }
            }
            case _ => Right(validationErrorNoMessage(rpcMessage.id))
        }
    }
}
