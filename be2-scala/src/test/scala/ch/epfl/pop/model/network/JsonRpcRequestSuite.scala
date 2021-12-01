package ch.epfl.pop.model.network

import org.scalatest.{FunSuite, Matchers}
import ch.epfl.pop.model.objects.{Base64Data, Channel, Hash, PublicKey, Timestamp}
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType, MessageData}
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.{Params, ParamsWithMessage}
import ch.epfl.pop.model.network.method.message.data.lao.CreateLao
import util.examples.MessageExample


class JsonRpcRequestSuite extends FunSuite with Matchers {
    final val messageEx: Message = MessageExample.MESSAGE
    final val messageLao: Message = MessageExample.MESSAGE_CREATELAO
    private final val laoId: String = "abcd"
    final val channelEx: Channel = Channel(Channel.rootChannelPrefix + laoId)

    test("Constructor works for regular Params and ParamsWithMessage"){
        val params: Params = new Params(channelEx)
        val paramsWithMessage: ParamsWithMessage = new ParamsWithMessage(channelEx, messageEx)
        val methodType: MethodType.MethodType = MethodType.BROADCAST
        val rpc: String = "rpc"
        val id: Option[Int] = Some(0)
        val rpcReq: JsonRpcRequest = JsonRpcRequest(rpc, methodType, params, id)

        rpcReq.jsonrpc should equal (rpc)
        rpcReq.method should equal (methodType)
        rpcReq.params should equal (params)
        rpcReq.id should equal (id)

        val rpcReq2: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithMessage, id)

        rpcReq2.params should equal (paramsWithMessage)
    }

    test("getParams returns right value"){
        val params: Params = new Params(channelEx)
        val paramsWithMessage: ParamsWithMessage = new ParamsWithMessage(channelEx, messageEx)
        val methodType: MethodType.MethodType = MethodType.BROADCAST
        val rpc: String = "rpc"
        val id: Option[Int] = Some(0)
        val rpcReq: JsonRpcRequest = JsonRpcRequest(rpc, methodType, params, id)

        rpcReq.getParams should equal (params)

        val rpcReq2: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithMessage, id)

        rpcReq2.getParams should equal (paramsWithMessage)
    }

    test("getParamsChannel returns right value"){
        val params: Params = new Params(channelEx)
        val paramsWithMessage: ParamsWithMessage = new ParamsWithMessage(channelEx, messageEx)
        val methodType: MethodType.MethodType = MethodType.BROADCAST
        val rpc: String = "rpc"
        val id: Option[Int] = Some(0)
        val rpcReq: JsonRpcRequest = JsonRpcRequest(rpc, methodType, params, id)

        rpcReq.getParamsChannel should equal (channelEx)

        val rpcReq2: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithMessage, id)

        rpcReq2.getParamsChannel should equal (channelEx)
    }

    test("hasParamsMessage returns right value"){
        val params: Params = new Params(channelEx)
        val paramsWithMessage: ParamsWithMessage = new ParamsWithMessage(channelEx, messageEx)
        val methodType: MethodType.MethodType = MethodType.BROADCAST
        val rpc: String = "rpc"
        val id: Option[Int] = Some(0)
        val rpcReq: JsonRpcRequest = JsonRpcRequest(rpc, methodType, params, id)

        rpcReq.hasParamsMessage should equal (false)

        val rpcReq2: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithMessage, id)

        rpcReq2.hasParamsMessage should equal (true)
    }

    test("getParamsMessage returns right value"){
        val params: Params = new Params(channelEx)
        val paramsWithMessage: ParamsWithMessage = new ParamsWithMessage(channelEx, messageEx)
        val methodType: MethodType.MethodType = MethodType.BROADCAST
        val rpc: String = "rpc"
        val id: Option[Int] = Some(0)
        val rpcReq: JsonRpcRequest = JsonRpcRequest(rpc, methodType, params, id)

        rpcReq.getParamsMessage should equal (None)

        val rpcReq2: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithMessage, id)

        rpcReq2.getParamsMessage should equal (Some(messageEx))
    }

    test("getEncodedData returns right value"){
        val params: Params = new Params(channelEx)
        val paramsWithMessage: ParamsWithMessage = new ParamsWithMessage(channelEx, messageEx)
        val methodType: MethodType.MethodType = MethodType.BROADCAST
        val rpc: String = "rpc"
        val id: Option[Int] = Some(0)
        val rpcReq: JsonRpcRequest = JsonRpcRequest(rpc, methodType, params, id)

        rpcReq.getEncodedData should equal (None)

        val rpcReq2: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithMessage, id)

        rpcReq2.getEncodedData should equal (Some(Base64Data("eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ==")))
    }

    test("getDecodedData returns right value"){
        val params: Params = new Params(channelEx)
        val paramsWithMessage: ParamsWithMessage = new ParamsWithMessage(channelEx, messageEx)
        val paramsWithMessageAndDecoded: ParamsWithMessage = new ParamsWithMessage(channelEx, messageLao)
        val methodType: MethodType.MethodType = MethodType.BROADCAST
        val rpc: String = "rpc"
        val id: Option[Int] = Some(0)
        val rpcReq: JsonRpcRequest = JsonRpcRequest(rpc, methodType, params, id)

        rpcReq.getDecodedData should equal (None)

        val rpcReq2: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithMessage, id)

        rpcReq2.getDecodedData should equal (None)

        val rpcReq3: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithMessageAndDecoded, id)

        rpcReq3.getDecodedData should equal (Some(CreateLao(Hash(Base64Data("id")), "LAO", Timestamp(0), PublicKey(Base64Data("key")), List.empty)))
    }

    test("getDecodedDataHeader returns right value"){
        val params: Params = new Params(channelEx)
        val paramsWithMessage: ParamsWithMessage = new ParamsWithMessage(channelEx, messageEx)
        val paramsWithMessageAndDecoded: ParamsWithMessage = new ParamsWithMessage(channelEx, messageLao)
        val methodType: MethodType.MethodType = MethodType.BROADCAST
        val rpc: String = "rpc"
        val id: Option[Int] = Some(0)
        val rpcReq: JsonRpcRequest = JsonRpcRequest(rpc, methodType, params, id)

        rpcReq.getDecodedDataHeader should equal (None)

        val rpcReq2: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithMessage, id)

        rpcReq2.getDecodedDataHeader should equal (None)

        val rpcReq3: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithMessageAndDecoded, id)

        rpcReq3.getDecodedDataHeader should equal (Some((ObjectType.LAO, ActionType.CREATE)))
    }

    test("setDecodedData sets data as intended"){
        val params: Params = new Params(channelEx)
        val messageToModify: Message = Message(messageEx.data, messageEx.sender, messageEx.signature, messageEx.message_id, messageEx.witness_signatures)
        val paramsWithMessage: ParamsWithMessage = new ParamsWithMessage(channelEx, messageToModify)
        val paramsWithMessageAndDecoded: ParamsWithMessage = new ParamsWithMessage(channelEx, messageLao)
        val methodType: MethodType.MethodType = MethodType.BROADCAST
        val rpc: String = "rpc"
        val id: Option[Int] = Some(0)
        val decodedData: MessageData = CreateLao(Hash(Base64Data("id")), "LAO", Timestamp(0), PublicKey(Base64Data("key")), List.empty)

        val rpcReq: JsonRpcRequest = JsonRpcRequest(rpc, methodType, params, id)
        rpcReq.setDecodedData(decodedData)

        rpcReq.getDecodedData should equal (None)

        val rpcReq2: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithMessage, id)
        rpcReq2.setDecodedData(decodedData)

        rpcReq2.getDecodedData should equal (Some(decodedData))
    }

    test("extractLaoId returns right id"){
        val params: Params = new Params(channelEx)
        val methodType: MethodType.MethodType = MethodType.BROADCAST
        val rpc: String = "rpc"
        val id: Option[Int] = Some(0)
        
        val rpcReq: JsonRpcRequest = JsonRpcRequest(rpc, methodType, params, id)

        rpcReq.extractLaoId should equal (Hash(Base64Data(laoId)))
    }
    
}
