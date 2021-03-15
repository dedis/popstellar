package ch.epfl.pop.model.network.method

import ch.epfl.pop.model.network.method.message.data.MessageData

case class Unsubscribe(channel: Channel) extends Params {
  override def buildFromJson(messageData: MessageData, payload: String): Unsubscribe = ???
}
