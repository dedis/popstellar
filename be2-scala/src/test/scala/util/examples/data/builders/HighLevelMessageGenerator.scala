package util.examples.data.builders

import ch.epfl.pop.model.network.{JsonRpcRequest, MethodType}
import ch.epfl.pop.model.network.method.ParamsWithMessage
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.network.method.message.data.rollCall.{CloseRollCall, CreateRollCall, OpenRollCall}
import ch.epfl.pop.model.network.requests.rollCall.{JsonRpcRequestCloseRollCall, JsonRpcRequestCreateRollCall, JsonRpcRequestOpenRollCall}
import ch.epfl.pop.model.objects.{Base64Data, Channel, Hash, PublicKey, Signature, WitnessSignaturePair}
import ch.epfl.pop.pubsub.graph.validators.RpcValidator

/**
  * Helper object to generate test data
  */
object HighLevelMessageGenerator {

  private val EMPTY_BASE_64 = Base64Data.encode("")

  /**
    * Helper class used to build Mid level protocol Messages
    *
    */
  sealed class MessageBuilder {
     /*** Builder params ***/
    private var data: Base64Data = EMPTY_BASE_64
    private var sender: PublicKey = PublicKey(EMPTY_BASE_64)
    private var signature: Signature = Signature(EMPTY_BASE_64)
    private var message_id: Hash = Hash(EMPTY_BASE_64)
    private var witness_signatures: List[WitnessSignaturePair] = Nil
    private var decodedData: Option[MessageData] = None

    def toMessage = new Message(data, sender, signature, message_id, witness_signatures, decodedData)

    def withSender(sender: PublicKey): MessageBuilder = {
      this.sender = sender
      this
    }

    def withSignature(signature: Signature): MessageBuilder = {
      this.signature = signature
      this
    }

    def withMessageId(message_id: Hash): MessageBuilder = {
      this.message_id = message_id
      this
    }

    def withDecodedData(decodedData: MessageData): MessageBuilder = {
      this.decodedData = Some(decodedData)
      this
    }

    def withWitnessSig(ws: WitnessSignaturePair): MessageBuilder = {
      this.witness_signatures = ws :: witness_signatures
      this
    }
  }

  /**
    * Helper class used to build High Level Messages with decoded and parsed data
    *
    * @param message mid-level message builder
    */
  sealed class HLMessageBuilder(var message: MessageBuilder){
    /*** Builder params ***/
    private var id = Some(1)
    private var payload: String = ""
    private var methodType:  MethodType.MethodType = null
    private var paramsChannel: Channel = Channel.ROOT_CHANNEL
    /**********************/
    private var messageData : MessageData = null
    private var params : ParamsWithMessage = null

    def withId(id: Int): HLMessageBuilder =  {
      this.id = Some(id)
      this
    }

    def withPayload(payload: String): HLMessageBuilder = {
      this.payload = payload
      this
    }

    def withMethodType(methodeType: MethodType.MethodType): HLMessageBuilder = {
      this.methodType = methodeType
      this
    }

    def withChannel(channel: Channel): HLMessageBuilder = {
      this.paramsChannel = channel
      this
    }

    /**
      * This methode must not be called before the payload and methodeType are set
      * @param objType conversion object type
      * @param actionType conversion action type
      * @return Typed High level JsonRpcRequest with decoded and parsed data (MessageData)
      */
    //TODO : implement other object types and actions
    def generateJsonRpcRequestWith(objType: ObjectType.ObjectType)(actionType: ActionType.ActionType): JsonRpcRequest = {

      assume(!payload.isBlank() &&  methodType != null)

      (objType, actionType) match {
        case (ObjectType.ROLL_CALL, ActionType.CREATE) =>
          messageData = CreateRollCall.buildFromJson(payload)
          params = new ParamsWithMessage(Channel.ROOT_CHANNEL, message.withDecodedData(messageData).toMessage)
          JsonRpcRequestCreateRollCall(RpcValidator.JSON_RPC_VERSION, methodType, params, id)

        case (ObjectType.ROLL_CALL, ActionType.OPEN)   =>
          messageData = OpenRollCall.buildFromJson(payload)
          params = new ParamsWithMessage(Channel.ROOT_CHANNEL, message.withDecodedData(messageData).toMessage)
          JsonRpcRequestOpenRollCall(RpcValidator.JSON_RPC_VERSION, methodType, params,id)

        case (ObjectType.ROLL_CALL, ActionType.CLOSE)  =>
          messageData = CloseRollCall.buildFromJson(payload)
          params = new ParamsWithMessage(Channel.ROOT_CHANNEL, message.withDecodedData(messageData).toMessage)
          JsonRpcRequestCloseRollCall(RpcValidator.JSON_RPC_VERSION, methodType, params,id)

        case(obj,act) => throw new IllegalStateException(s"HLMessageBuilder failed: ($obj, $act) not implemented yet !!")
      }
    }
  }
}
