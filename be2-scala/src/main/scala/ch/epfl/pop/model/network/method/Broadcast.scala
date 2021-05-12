package ch.epfl.pop.model.network.method

import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.MessageData
import ch.epfl.pop.model.objects.Channel

import ch.epfl.pop.jsonNew.HighLevelProtocol._
import spray.json._

case class Broadcast(override val channel: Channel, override val message: Message) extends ParamsWithMessage(channel, message)

object Broadcast extends Parsable {
  def apply(channel: Channel, message: Message): Broadcast = {
    new Broadcast(channel, message)
  }

  override def buildFromJson(messageData: MessageData, payload: String): Broadcast =
    payload.parseJson.asJsObject.convertTo[Broadcast]
}
