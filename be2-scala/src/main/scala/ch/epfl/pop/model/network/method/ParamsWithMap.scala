package ch.epfl.pop.model.network.method

import ch.epfl.pop.model.objects.{Channel, Hash}

class ParamsWithMap(val channelsToMessageIds: Map[Channel, Set[Hash]]) extends Params {
  def hasChannel: Boolean = false
  def hasMessage: Boolean = false
}
