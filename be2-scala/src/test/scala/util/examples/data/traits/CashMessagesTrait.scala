package util.examples.data.traits

import ch.epfl.pop.model.network.MethodType
import ch.epfl.pop.model.network.method.message.data.ObjectType

/**
 * Trait to be implemented by cash system message examples
 */
trait CashMessagesTrait extends ExampleMessagesTrait {
  override val obj = ObjectType.TRANSACTION
  override val METHOD_TYPE = MethodType.BROADCAST
}
