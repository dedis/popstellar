package util.examples.Rumor

import ch.epfl.pop.model.network.method.Rumor
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.{Base64Data, Channel, PublicKey}
import util.examples.MessageExample

object RumorExample {

  private val senderPk: PublicKey = PublicKey(Base64Data.encode("publicKey"))
  private val channel: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX+"rumorExample")
  private val messages: List[Message] = (for i <- 0 until 10 yield MessageExample.MESSAGE).toList
  val rumorExample : Rumor = Rumor(senderPk, 1, Map(channel -> messages))



}
