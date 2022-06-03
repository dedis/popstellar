package util.examples.data.traits

import ch.epfl.pop.model.network.MethodType
import ch.epfl.pop.model.network.method.message.data.ObjectType

/**
 * Trait to be implemented by coin system message examples
 */
trait CoinMessagesTrait extends ExampleMessagesTrait {
  override val obj = ObjectType.COIN
  override val METHOD_TYPE = MethodType.BROADCAST
}
