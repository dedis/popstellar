package util.examples

import MessageExample._
import ch.epfl.pop.model.network.{JsonRpcRequest, MethodType}
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.{Params, ParamsWithMessage}
import ch.epfl.pop.model.objects.Channel

object JsonRpcRequestExample {

    private final val rpc: String = "rpc"
    private final val id: Option[Int] = Some(0)
    private final val methodType: MethodType.MethodType = MethodType.PUBLISH
    private final val channel: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + "channel")
    private final val paramsWithoutMessage: Params = new Params(channel)
    private final val paramsWithMessage: ParamsWithMessage = new ParamsWithMessage(channel, MESSAGE_WORKING_WS_PAIR)
    private final val paramsWithFaultyIdMessage: ParamsWithMessage = new ParamsWithMessage(channel, MESSAGE_FAULTY_ID)
    private final val paramsWithFaultyWSMessage: ParamsWithMessage = new ParamsWithMessage(channel, MESSAGE_FAULTY_WS_PAIR)
    private final val paramsWithFaultySignatureMessage: ParamsWithMessage = new ParamsWithMessage(channel, MESSAGE_FAULTY_SIGNATURE)

    final val VALID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithMessage, id)
    final val INVALID_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithFaultyIdMessage, id)
    final val INVALID_WS_PAIR_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithFaultyWSMessage, id)
    final val INVALID_SIGNATURE_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithFaultySignatureMessage, id)
    final val RPC_NO_PARAMS: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithoutMessage, id)

    //for CreateLao testing
    private final val paramsWithCreateLao: ParamsWithMessage = new ParamsWithMessage(Channel.ROOT_CHANNEL, MESSAGE_CREATELAO_WORKING)
    private final val paramsWithCreateLaoWrongTimestamp: ParamsWithMessage = new ParamsWithMessage(Channel.ROOT_CHANNEL, MESSAGE_CREATELAO_WRONG_TIMESTAMP)
    private final val paramsWithCreateLaoWrongChannel: ParamsWithMessage = new ParamsWithMessage(channel, MESSAGE_CREATELAO_WORKING)
    private final val paramsWithCreateLaoWrongWitnesses: ParamsWithMessage = new ParamsWithMessage(Channel.ROOT_CHANNEL, MESSAGE_CREATELAO_WRONG_WITNESSES)
    private final val paramsWithCreateLaoWrongId: ParamsWithMessage = new ParamsWithMessage(Channel.ROOT_CHANNEL, MESSAGE_CREATELAO_WRONG_ID)
    private final val paramsWithCreateLaoWrongSender: ParamsWithMessage = new ParamsWithMessage(Channel.ROOT_CHANNEL, MESSAGE_CREATELAO_WRONG_SENDER)
    final val CREATE_LAO_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateLao, id)
    final val CREATE_LAO_WRONG_CHANNEL_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateLaoWrongChannel, id)
    final val CREATE_LAO_WRONG_TIMESTAMP_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateLaoWrongTimestamp, id)
    final val CREATE_LAO_WRONG_WITNESSES_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateLaoWrongWitnesses, id)
    final val CREATE_LAO_WRONG_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateLaoWrongId, id)
    final val CREATE_LAO_WRONG_SENDER_RPC: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithCreateLaoWrongSender, id)

}
