package util.examples.data.traits

import ch.epfl.pop.model.network.MethodType
import ch.epfl.pop.model.network.method.message.data.ObjectType

/** Trait to be implemented by ElectionMessages examples
  */
trait ElectionMessagesTrait extends ExampleMessagesTrait {
  override val obj: ObjectType = ObjectType.election
  override val METHOD_TYPE: MethodType = MethodType.publish

}
