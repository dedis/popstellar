package ch.epfl.pop.model.network.method

import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.MessageData

case class Publish(channel: Channel, message: Message) extends ParamsWithMessage {
  override def buildFromJson(messageData: MessageData, payload: String): Publish = ???
}
