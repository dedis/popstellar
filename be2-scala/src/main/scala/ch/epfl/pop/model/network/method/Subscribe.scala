package ch.epfl.pop.model.network.method

import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.MessageData
import ch.epfl.pop.model.objects.Channel.Channel

import ch.epfl.pop.jsonNew.HighLevelProtocol._
import spray.json._

case class Subscribe(override val channel: Channel) extends Params(channel)

object Subscribe extends Parsable {
  def apply(channel: Channel): Subscribe = {
    new Subscribe(channel)
  }

  override def buildFromJson(messageData: MessageData, payload: String): Subscribe =
    payload.parseJson.asJsObject.convertTo[Subscribe]
}
