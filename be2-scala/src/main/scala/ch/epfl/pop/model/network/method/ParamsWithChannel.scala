package ch.epfl.pop.model.network.method

import ch.epfl.pop.model.objects.Channel

class ParamsWithChannel(override val channel: Channel) extends Params {
  def hasMessage: Boolean = false
}
