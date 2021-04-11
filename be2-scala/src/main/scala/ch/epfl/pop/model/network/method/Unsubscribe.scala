package ch.epfl.pop.model.network.method

import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.MessageData
import ch.epfl.pop.model.objects.Channel.Channel

case class Unsubscribe(channel: Channel) extends Params

object Unsubscribe extends Parsable {
  def apply(channel: Channel): Unsubscribe = {
    // FIXME add checks
    new Unsubscribe(channel)
  }

  override def buildFromJson(messageData: MessageData, payload: String): Unsubscribe = ???
}
