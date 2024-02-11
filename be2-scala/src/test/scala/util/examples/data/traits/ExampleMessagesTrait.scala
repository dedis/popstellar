package util.examples.data.traits

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.MethodType.MethodType
import ch.epfl.pop.model.network.method.message.data.ActionType._
import ch.epfl.pop.model.network.method.message.data.ObjectType._
import ch.epfl.pop.model.objects.{Base64Data, Channel, PublicKey}
import util.examples.data.builders.HighLevelMessageGenerator

/** Trait to be extended by other Feature traits
  */
trait ExampleMessagesTrait {

  import ExampleMessages._

  // Default Channel
  val CHANNEL: Channel = DEFAULT_CHANNEL

  // Object and Action type of the message
  val obj: ObjectType
  val action: ActionType

  val METHOD_TYPE: MethodType

  /** @param fileName
    *   file name of the .json file to get the payload from
    * @param messageBuilder
    *   mid level Message to use as base for HighLevelMessage
    * @return
    *   a parsed and decoded JsonRpcRequest
    */
  def getJsonRPCRequestFromFile(fileName: String)(messageBuilder: HighLevelMessageGenerator.HLMessageBuilder = DEFAULT_HL_MESSAGE_BUILDER): JsonRpcRequest = {
    val payload = getPayloadFromFile(fileName)
    buildJsonRpcRequest(payload)(messageBuilder)
  }

  /** Builds json request given a data payload and predefined high level message builder
    *
    * @param payload
    *   the payload to parse
    * @param highLevelMessage
    *   high level message builder for the payload
    * @return
    *   a parsed and decoded JsonRpcRequest
    */
  def buildJsonRpcRequest(payload: String)(highLevelMessage: HighLevelMessageGenerator.HLMessageBuilder): JsonRpcRequest = {
    highLevelMessage.withMethodType(METHOD_TYPE).withChannel(CHANNEL).withPayload(payload).generateJsonRpcRequestWith(obj)(action)
  }
}

object ExampleMessages {
  val DEFAULT_BASE_PATH = "../protocol/examples/messageData"
  val DEFAULT_SENDER: PublicKey = PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM="))
  val DEFAULT_CHANNEL: Channel = Channel.ROOT_CHANNEL

  // Default builder used mid level Message
  final val DEFAULT_MESSAGE_BUILDER: HighLevelMessageGenerator.MessageBuilder = new HighLevelMessageGenerator.MessageBuilder().withSender(DEFAULT_SENDER)
  final val DEFAULT_HL_MESSAGE_BUILDER: HighLevelMessageGenerator.HLMessageBuilder = new HighLevelMessageGenerator.HLMessageBuilder(DEFAULT_MESSAGE_BUILDER).withId(1).withChannel(DEFAULT_CHANNEL)

  // Reads and returns payload from .json file
  final def getPayloadFromFile(fileName: String): String = {
    val source = scala.io.Source.fromFile(s"$DEFAULT_BASE_PATH/$fileName")
    try source.mkString
    finally source.close()
  }

}
