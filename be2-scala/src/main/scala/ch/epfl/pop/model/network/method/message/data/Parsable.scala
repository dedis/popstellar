package ch.epfl.pop.model.network.method.message.data

trait Parsable {
  def buildFromJson(messageData: MessageData, payload: String): Any
}
