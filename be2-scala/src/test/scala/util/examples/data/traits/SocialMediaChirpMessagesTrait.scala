package util.examples.data.traits

import ch.epfl.pop.model.network.MethodType
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType

/** Trait to be implemented by SocialMediaMessages examples (chirps)
  */
trait SocialMediaChirpMessagesTrait extends ExampleMessagesTrait {
  override val obj: ObjectType = ObjectType.CHIRP
  override val METHOD_TYPE: MethodType.MethodType = MethodType.PUBLISH

}
