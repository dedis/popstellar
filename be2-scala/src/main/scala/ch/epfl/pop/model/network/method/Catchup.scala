package ch.epfl.pop.model.network.method

import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.MessageData
import ch.epfl.pop.model.objects.Channel.Channel

import ch.epfl.pop.jsonNew.HighLevelProtocol._
import spray.json._

case class Catchup(channel: Channel) extends Params

object Catchup extends Parsable {
  def apply(channel: Channel): Catchup = {
    new Catchup(channel)
  }

  override def buildFromJson(messageData: MessageData, payload: String): Catchup =
    payload.parseJson.asJsObject.convertTo[Catchup]
}
