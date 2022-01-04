package util.examples.dataTraits

import util.examples.ExampleMessagesTrait
import java.nio.file.Path
import ch.epfl.pop.model.network.method.message.data.ActionType
import ch.epfl.pop.model.network.JsonRpcRequest
import java.nio.file.Files
import util.examples.HighLevelMessageGenerator
import ch.epfl.pop.model.network.MethodType
import ch.epfl.pop.model.network.method.message.data.ObjectType

/**
  * Trait to be implemented by RollCallMessages exapmles
  */
trait RollCallMessagesTrait extends ExampleMessagesTrait {
  override val obj = ObjectType.ROLL_CALL
  override val METHOD_TYPE: MethodType.MethodType = MethodType.PUBLISH

}
