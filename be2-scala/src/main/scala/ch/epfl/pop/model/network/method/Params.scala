package ch.epfl.pop.model.network.method

import ch.epfl.pop.model.network.Parsable

trait Params extends Parsable {
  val channel: Channel
}
