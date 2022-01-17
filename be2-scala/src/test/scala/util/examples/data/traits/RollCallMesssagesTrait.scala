package util.examples.data.traits

import ch.epfl.pop.model.network.method.message.data.{ObjectType,ActionType}

import ch.epfl.pop.model.network.{JsonRpcRequest,MethodType}
import util.examples.data.builders.HighLevelMessageGenerator
import util.examples.data.traits.ExampleMessagesTrait

import java.nio.file.Path
import java.nio.file.Files

/**
  * Trait to be implemented by RollCallMessages examples
  */
trait RollCallMessagesTrait extends ExampleMessagesTrait {
  override val obj = ObjectType.ROLL_CALL
  override val METHOD_TYPE: MethodType.MethodType = MethodType.PUBLISH

}
