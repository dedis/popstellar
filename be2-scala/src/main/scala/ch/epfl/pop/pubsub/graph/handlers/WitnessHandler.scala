package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.witness.WitnessMessage
import ch.epfl.pop.model.network.requests.witness.JsonRpcRequestWitnessMessage
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.model.objects.{Hash, WitnessSignaturePair}
import ch.epfl.pop.pubsub.graph.DbActor.{DbActorNAck, DbActorWriteAck}
import ch.epfl.pop.pubsub.graph.{DbActor, ErrorCodes, GraphMessage, PipelineError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

case object WitnessHandler extends MessageHandler {

  override val handler: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Left(jsonRpcMessage) => jsonRpcMessage match {
      case message@(_: JsonRpcRequestWitnessMessage) => handleWitnessMessage(message)
      case _ => Right(PipelineError(
        ErrorCodes.SERVER_ERROR.id,
        "Internal server fault: WitnessHandler was given a message it could not recognize",
        jsonRpcMessage match {
          case r: JsonRpcRequest => r.id
          case r: JsonRpcResponse => r.id
          case _ => None
        }
      ))
    }
    case graphMessage@_ => graphMessage
  }

  def handleWitnessMessage(rpcMessage: JsonRpcRequest): GraphMessage = {
    val decodedData: WitnessMessage = rpcMessage.getDecodedData.get.asInstanceOf[WitnessMessage]
    val messageId: Hash = decodedData.message_id

    // get message (Message) with messageId message_id from db
    var message: Message = ???

    // add new witness signature to existing ones
    message = message.addWitnessSignature(WitnessSignaturePair(message.sender, message.signature))

    // overwrite message in db
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage, message)
    Await.result(ask, duration)
  }
}
