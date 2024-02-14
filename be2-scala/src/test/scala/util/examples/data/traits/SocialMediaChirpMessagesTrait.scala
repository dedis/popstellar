package util.examples.data.traits

import ch.epfl.pop.model.network.MethodType
import ch.epfl.pop.model.network.method.message.data.ObjectType

/** Trait to be implemented by SocialMediaMessages examples (chirps)
  */
trait SocialMediaChirpMessagesTrait extends ExampleMessagesTrait {
  override val obj: ObjectType = ObjectType.chirp
  override val METHOD_TYPE: MethodType = MethodType.publish

}
