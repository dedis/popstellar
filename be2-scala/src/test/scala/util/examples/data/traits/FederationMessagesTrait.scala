package util.examples.data.traits

import ch.epfl.pop.model.network.MethodType
import ch.epfl.pop.model.network.method.message.data.ObjectType

/** Trait to be implemented by FederationMessages examples
  */
trait FederationMessagesTrait extends ExampleMessagesTrait {
  override val obj: ObjectType = ObjectType.federation
  override val METHOD_TYPE: MethodType = MethodType.publish 

}
