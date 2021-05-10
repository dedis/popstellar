package ch.epfl.pop.model.network.method

import ch.epfl.pop.model.objects.Channel.Channel

trait Params {
  val channel: Channel
  // Note: Parsable enforced in companion objects
}
