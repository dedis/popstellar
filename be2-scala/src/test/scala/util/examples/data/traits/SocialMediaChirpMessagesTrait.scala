package util.examples.data.traits

import ch.epfl.pop.model.network.MethodType
import ch.epfl.pop.model.network.method.message.data.ObjectType

/**
  * Trait to be implemented by SocialMediaMessages examples (chirps)
  */
trait SocialMediaChirpMessagesTrait extends ExampleMessagesTrait {
  override val obj = ObjectType.CHIRP
  override val METHOD_TYPE: MethodType.MethodType = MethodType.PUBLISH

}
