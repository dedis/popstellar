package ch.epfl.pop.pubsub.graph.handlers

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.witness.WitnessMessage
import ch.epfl.pop.model.objects.{Channel, DbActorNAckException, Hash, Signature}
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.DbActorAddWitnessSignatureAck

import scala.concurrent.Await
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

/** WitnessHandler object uses the db instance from the MessageHandler
  */
object WitnessHandler {
  final lazy val handlerInstance = {
    println(s"new WitnessHandler ${DbActor.getInstance.actorRef}")
    new WitnessHandler(DbActor.getInstance)
  }

  def handleWitnessMessage(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleWitnessMessage(rpcMessage)
}

class WitnessHandler(dbRef: => AskableActorRef) extends MessageHandler {

  /** Overrides default DbActor with provided parameter
    */
  override final val dbActor: AskableActorRef = dbRef

  def handleWitnessMessage(rpcMessage: JsonRpcRequest): GraphMessage = {
    val combined =
      for {
        case (_, _, decodedData) <- extractParameters[WitnessMessage](rpcMessage, s"Unable to handle witness message $rpcMessage. Not an AddWitnessSignature message")
        messageId: Hash = decodedData.get.message_id
        signature: Signature = decodedData.get.signature
        channel: Channel = rpcMessage.getParamsChannel
        // add new witness signature to existing ones
        case DbActorAddWitnessSignatureAck(witnessMessage) <- dbRef ? DbActor.AddWitnessSignature(channel, messageId, signature)
        // overwrites the message containing now the witness signature in the db
        _ <- dbRef ? DbActor.Write(channel, witnessMessage)
        // propagate signature message only
        _ <- dbRef ? DbActor.WriteAndPropagate(channel, rpcMessage.getParamsMessage.get)
      } yield ()

    Await.ready(combined, duration).value.get match {
      case Success(_)                        => Right(rpcMessage)
      case Failure(ex: DbActorNAckException) => Left(PipelineError(ex.code, s"handleWitnessMessage failed : ${ex.message}", rpcMessage.getId))
      case reply                             => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleWitnessMessage failed : unknown DbActor reply $reply", rpcMessage.getId))
    }
  }
}
