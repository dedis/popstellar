package util.examples.data.traits

import ch.epfl.pop.model.network.MethodType
import ch.epfl.pop.model.network.method.message.data.ObjectType

/** Trait to be implemented by WitnessMessage examples
  */
trait WitnessMessagesTrait extends ExampleMessagesTrait {
  override val obj: ObjectType = ObjectType.message
  override val METHOD_TYPE: MethodType = MethodType.publish

}
