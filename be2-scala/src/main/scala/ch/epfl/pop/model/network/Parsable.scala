package ch.epfl.pop.model.network

import ch.epfl.pop.model.network.method.message.data.MessageData

trait Parsable {
  def buildFromJson(messageData: MessageData, payload: String): Any
}
