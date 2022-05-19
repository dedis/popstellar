package ch.epfl.pop.pubsub.graph.handlers

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.witness.WitnessMessage
import ch.epfl.pop.model.objects.{Channel, DbActorNAckException, Hash, Signature, WitnessSignaturePair}
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

import scala.concurrent.Await
import scala.util.{Failure, Success}

case object WitnessHandler extends MessageHandler {

  def handleWitnessMessage(rpcMessage: JsonRpcRequest): GraphMessage = {
    val decodedData: WitnessMessage = rpcMessage.getDecodedData.get.asInstanceOf[WitnessMessage]
    val messageId: Hash = decodedData.message_id
    val signature: Signature = decodedData.signature
    val channel: Channel = rpcMessage.getParamsChannel

    rpcMessage.getParamsMessage match {
      case Some(witnessMessage) =>
        // get message (Message) with messageId message_id from db
        val askRead = dbActor ? DbActor.Read(channel, messageId)
        Await.ready(askRead, duration).value match {
          case Some(Success(DbActor.DbActorReadAck(Some(message)))) =>
            // add new witness signature to existing ones
            message.addWitnessSignature(WitnessSignaturePair(witnessMessage.sender, signature))
            val askWritePropagate = dbActor ? DbActor.WriteAndPropagate(channel, message)
            Await.result(askWritePropagate, duration) match {
              case Success(_) => Left(rpcMessage)
              case Failure(ex: DbActorNAckException) => Right(PipelineError(ex.code, s"handleWitnessMessage failed : ${ex.message}", rpcMessage.getId))
              case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleWitnessMessage failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
            }
          case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"handleWitnessMessage failed : ${ex.message}", rpcMessage.getId))
          case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleWitnessMessage failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
        }
      case _ => Right(PipelineError(
        ErrorCodes.SERVER_ERROR.id,
        s"Unable to handle witness message $rpcMessage. Not a AddWitness message",
        rpcMessage.id
      ))
    }
  }
}
