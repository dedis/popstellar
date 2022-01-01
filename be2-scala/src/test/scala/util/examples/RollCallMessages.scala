package util.examples

import ch.epfl.pop.model.network.method.message.data.{ObjectType, ActionType}
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.MethodType
import ch.epfl.pop.model.network.method.ParamsWithMessage
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.rollCall.CreateRollCall
import ch.epfl.pop.model.network.requests.rollCall.JsonRpcRequestCreateRollCall

import ch.epfl.pop.model.objects.{Channel, Signature, Base64Data, PublicKey, Hash, WitnessSignaturePair}
import ch.epfl.pop.pubsub.graph.validators.RpcValidator

import java.nio.file.{Files, Path}

/**
  * Generates high level RollCall Messages from protocol folder
  * Content validation : all the params are required
  * Handling : id, message with decoded data, channel are required
  */
object RollCallMessages {

  private final val SENDER = PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM="))

  //JsonRpcCreateRollCall message greneration
  //Should be modified if necessary to build/re-build with custom parameters/arguments using builders functions
  final val createRollCall: JsonRpcRequest = {
    val payload = Files.readString(Path.of("..", "protocol","examples","messageData","roll_call_create.json"))
    //Build mid level Message with builder
    val messageBuilder = new HighLevelMessageGenerator.MessageBuilder().withSender(SENDER) //withData, withMessageId...

    //Build high level message dta with params given a pre-buit Message
    val highLevelMessage = new HighLevelMessageGenerator.HLMessageBuilder(messageBuilder)
      .withPayload(payload).withMethodType(MethodType.PUBLISH)
      //withChannel, withId....

    highLevelMessage.generateJsonRpcRequestWith(ObjectType.ROLL_CALL)(ActionType.CREATE)
  }

  //TODO: Generate other RollCall messages
}
