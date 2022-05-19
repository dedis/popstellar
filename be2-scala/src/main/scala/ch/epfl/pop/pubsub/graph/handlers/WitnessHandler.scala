package ch.epfl.pop.pubsub.graph.handlers

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.witness.WitnessMessage
import ch.epfl.pop.model.objects.{Channel, DbActorNAckException, Hash, Signature, WitnessSignaturePair}
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.DbActorAddWitnessMessage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.util.{Failure, Success}

case object WitnessHandler extends MessageHandler {

  def handleWitnessMessage(rpcMessage: JsonRpcRequest): GraphMessage = {
    val decodedData: WitnessMessage = rpcMessage.getDecodedData.get.asInstanceOf[WitnessMessage]
    val messageId: Hash = decodedData.message_id
    val signature: Signature = decodedData.signature
    val channel: Channel = rpcMessage.getParamsChannel

    rpcMessage.getParamsMessage match {
      case Some(_) =>
        val combined = for {
          // add new witness signature to existing ones
          DbActorAddWitnessMessage(witnessMessage) <- dbActor ? DbActor.AddWitnessSignature(channel, messageId, signature)
          //overwrites the message containing now the witness signature in the db
          _ <- dbActor ? DbActor.WriteAndPropagate(channel, witnessMessage)
        } yield ()

        Await.ready(combined, duration).value match {
          case Some(Success(_)) => Left(rpcMessage)
          case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"handleWitnessMessage failed : ${ex.message}", rpcMessage.getId))
          case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleWitnessMessage failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
        }

      case _ => Right(PipelineError(
        ErrorCodes.SERVER_ERROR.id,
        s"Unable to handle witness message $rpcMessage. Not a AddWitnessSignature message",
        rpcMessage.id
      ))
    }
  }
}
