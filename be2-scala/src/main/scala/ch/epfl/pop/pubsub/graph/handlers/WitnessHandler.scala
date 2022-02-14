package ch.epfl.pop.pubsub.graph.handlers

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.witness.WitnessMessage
import ch.epfl.pop.model.objects.{Hash, WitnessSignaturePair}
import ch.epfl.pop.pubsub.graph.GraphMessage

import scala.concurrent.{Await, Future}

case object WitnessHandler extends MessageHandler {

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
