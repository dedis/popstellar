package ch.epfl.pop.model.network.method

import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.MessageData
import ch.epfl.pop.model.objects.Channel.Channel

case class Catchup(channel: Channel) extends Params

object Catchup extends Parsable {
  def apply(channel: Channel): Catchup = {
    // FIXME add checks
    new Catchup(channel)
  }

  override def buildFromJson(messageData: MessageData, payload: String): Catchup = ???
}
