package ch.epfl.pop.model.network.method.message

import ch.epfl.pop.json.HighLevelProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.MessageData
import ch.epfl.pop.model.objects._
import spray.json._

final case class Message(
                          data: Base64Data,
                          sender: PublicKey,
                          signature: Signature,
                          message_id: Hash,
                          witness_signatures: List[WitnessSignaturePair],
                          decodedData: Option[MessageData]
                        ) {
  def addWitnessSignature(ws: WitnessSignaturePair): Message =
    Message(data, sender, signature, message_id, ws :: witness_signatures, decodedData)

  def toJsonString: String = {
    val that: Message = this // tricks the compiler into inferring the right type
    that.toJson.toString
  }
}

object Message extends Parsable {
  def apply(
             data: Base64Data,
             sender: PublicKey,
             signature: Signature,
             message_id: Hash,
             witness_signatures: List[WitnessSignaturePair]
           ): Message = {
    new Message(data, sender, signature, message_id, witness_signatures, None)
  }

  override def buildFromJson(payload: String): Message = payload.parseJson.asJsObject.convertTo[Message] // doesn't decode data
}
