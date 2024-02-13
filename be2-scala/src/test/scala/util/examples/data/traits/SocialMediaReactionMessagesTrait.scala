package util.examples.data.traits

import ch.epfl.pop.model.network.MethodType
import ch.epfl.pop.model.network.method.message.data.ObjectType

/** Trait to be implemented by SocialMediaMessages examples (reactions)
  */
trait SocialMediaReactionMessagesTrait extends ExampleMessagesTrait {
  override val obj: ObjectType = ObjectType.REACTION
  override val METHOD_TYPE: MethodType = MethodType.PUBLISH

}
