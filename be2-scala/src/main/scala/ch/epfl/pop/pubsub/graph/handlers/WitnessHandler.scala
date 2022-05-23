package ch.epfl.pop.pubsub.graph.handlers

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.witness.WitnessMessage
import ch.epfl.pop.model.objects.{Channel, DbActorNAckException, Hash, Signature}
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

import scala.concurrent.Await
import scala.util.{Failure, Success}

/**
 * WitnessHandler object uses the db instance from the MessageHandler
 */
object WitnessHandler extends MessageHandler {
  final lazy val handlerInstance = new WitnessHandler(super.dbActor)
  def handleWitnessMessage(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleWitnessMessage(rpcMessage)
}


class WitnessHandler(dbRef: => AskableActorRef) extends MessageHandler {

  /**
   *
   * Overrides default DbActor with provided parameter
   */
  override final val dbActor: AskableActorRef = dbRef

  def handleWitnessMessage(rpcMessage: JsonRpcRequest): GraphMessage = {
    val decodedData: WitnessMessage = rpcMessage.getDecodedData.get.asInstanceOf[WitnessMessage]
    val messageId: Hash = decodedData.message_id
    val signature: Signature = decodedData.signature
    val channel: Channel = rpcMessage.getParamsChannel

    rpcMessage.getParamsMessage match {
      case Some(_) =>
          // add new witness signature to existing ones
          val askAddWitness =  dbActor ? DbActor.AddWitnessSignature(channel, messageId, signature)
          Await.ready(askAddWitness, duration).value match {
            case Some(Success(DbActor.DbActorAddWitnessMessage(witnessMessage))) =>
              //overwrites the message containing now the witness signature in the db
              val askWritePropagate = dbActor ? DbActor.WriteAndPropagate(channel, witnessMessage)
              Await.ready(askWritePropagate, duration).value.get match {
                case Success(_) => Left(rpcMessage)
                case Failure(ex: DbActorNAckException) => Right(PipelineError(ex.code, s"handleWitnessMessage failed : ${ex.message}", rpcMessage.getId))
                case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleWitnessMessage failed : unknown DbActor reply $reply", rpcMessage.getId))
              }
            case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"handleWitnessMessage failed : ${ex.message}", rpcMessage.getId))
            case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleWitnessMessage failed : unknown DbActor reply $reply", rpcMessage.getId))
          }

      case _ => Right(PipelineError(
        ErrorCodes.SERVER_ERROR.id,
        s"Unable to handle witness message $rpcMessage. Not an AddWitnessSignature message",
        rpcMessage.id
      ))
    }
  }
}
