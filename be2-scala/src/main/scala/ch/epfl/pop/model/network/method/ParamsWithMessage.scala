package ch.epfl.pop.model.network.method

import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.Channel

class ParamsWithMessage(override val channel: Channel, val message: Message) extends ParamsWithChannel(channel) {
  override def hasMessage: Boolean = true
}
