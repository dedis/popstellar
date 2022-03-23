package ch.epfl.pop.pubsub.graph.handlers

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.witness.WitnessMessage
import ch.epfl.pop.model.objects.{Channel, DbActorNAckException, Hash, WitnessSignaturePair}
import ch.epfl.pop.pubsub.graph.handlers.ElectionHandler.{dbActor, duration}
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

case object WitnessHandler extends MessageHandler {

  def handleWitnessMessage(rpcMessage: JsonRpcRequest): GraphMessage = {
    val decodedData: WitnessMessage = rpcMessage.getDecodedData.get.asInstanceOf[WitnessMessage]
    val messageId: Hash = decodedData.message_id

    // get message (Message) with messageId message_id from db
    val message: Future[Message] = dbActor ? DbActor.Read(Channel.ROOT_CHANNEL, messageId) map {
      case Success(Some(message: Message)) =>
        message.addWitnessSignature(WitnessSignaturePair(message.sender, message.signature))
      case _ =>
        // TODO
        throw DbActorNAckException(ErrorCodes.INVALID_ACTION.id, "message doesn't exist in database")
    }

    val combined = for {
      msg <- message
      _ <- dbAskWritePropagate(???) // TODO create request with message
    } yield ()

    Await.ready(combined, duration).value match {
      case Some(Success(_)) => Left(rpcMessage)
      case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"handleWitnessMessage failed : ${ex.message}", rpcMessage.getId))
      case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleSetupElection failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
    }
  }
}
