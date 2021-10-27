package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.socialMedia.{AddChirp}
import ch.epfl.pop.model.objects.Hash
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}

case object SocialMediaValidator extends MessageDataContentValidator with EventValidator {
    override def EVENT_HASH_PREFIX: String = "SocialMedia" //to check later on

    def validateAddChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
        def validationError(reason: String): PipelineError = super.validationError(reason, "AddChirp", rpcMessage.id)

        rpcMessage.getParamsMessage match {
            // FIXME need more checks
            case Some(_) => Left(rpcMessage)
            case _ => Right(validationErrorNoMessage(rpcMessage.id))
        }
    }
}