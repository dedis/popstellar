package util.examples.data.traits

import ch.epfl.pop.model.network.MethodType
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType

/** Trait to be implemented by RollCallMessages examples
  */
trait RollCallMessagesTrait extends ExampleMessagesTrait {
  override val obj: ObjectType = ObjectType.ROLL_CALL
  override val METHOD_TYPE: MethodType.MethodType = MethodType.PUBLISH

}
