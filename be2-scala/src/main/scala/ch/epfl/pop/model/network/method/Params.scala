package ch.epfl.pop.model.network.method

import ch.epfl.pop.model.objects.Channel

abstract class Params {
  // Default channel
  val channel: Channel = Channel.ROOT_CHANNEL
  def hasMessage: Boolean

}
