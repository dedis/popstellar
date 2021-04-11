package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.witness.WitnessMessage
import ch.epfl.pop.model.network.requests.witness.JsonRpcRequestWitnessMessage
import ch.epfl.pop.model.objects.{Hash, WitnessSignaturePair}
import ch.epfl.pop.pubsub.graph.{DbActorNew, ErrorCodes, GraphMessage, PipelineError}

import scala.util.Success

case object WitnessHandler extends MessageHandler {

  override val handler: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Left(jsonRpcMessage) => jsonRpcMessage match {
      case message@(_: JsonRpcRequestWitnessMessage) => handleWitnessMessage(message)
      case _ => Right(PipelineError(
        ErrorCodes.SERVER_FAULT.id,
        "Internal server fault: WitnessHandler was given a message it could not recognize"
      ))
    }
    case graphMessage@_ => graphMessage
  }

  def handleWitnessMessage(rpcMessage: JsonRpcRequest): GraphMessage = {
    val decodedData: WitnessMessage = rpcMessage.getDecodedData.get.asInstanceOf[WitnessMessage]
    val messageId: Hash = decodedData.message_id
    dbActor.ask(ref => DbActorNew.Read(rpcMessage.getParamsChannel, messageId, ref)) match {
      case Some(message: Message) => dbActor.ask(ref => DbActorNew.Write(
        rpcMessage.getParamsChannel,
        message.addWitnessSignature(WitnessSignaturePair(rpcMessage.getParamsMessage.get.sender, decodedData.signature)),
        ref
      )) match {
        case Success(_) =>
          // FIXME propagate
          Left(rpcMessage)
        case _ => Right(PipelineError(-10, "")) // FIXME add DbActor "answers" with error description if failed
      }
      case None => Right(PipelineError(-10, "")) // FIXME add DbActor "answers" with error description if failed
    }
  }
}
