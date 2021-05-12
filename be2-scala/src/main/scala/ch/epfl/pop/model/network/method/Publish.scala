package ch.epfl.pop.model.network.method

import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.MessageData
import ch.epfl.pop.model.objects.Channel.Channel

import ch.epfl.pop.jsonNew.HighLevelProtocol._
import spray.json._

case class Publish(override val channel: Channel, override val message: Message) extends ParamsWithMessage(channel, message)

object Publish extends Parsable {
  def apply(channel: Channel, message: Message): Publish = {
    new Publish(channel, message)
  }

  override def buildFromJson(messageData: MessageData, payload: String): Publish =
    payload.parseJson.asJsObject.convertTo[Publish]
}
