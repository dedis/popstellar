package ch.epfl.pop.model.network

import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.lao.CreateLao
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.network.method.{Params, ParamsWithMessage}
import ch.epfl.pop.model.objects._
import org.scalatest.{FunSuite, Matchers}
import util.examples.{JsonRpcRequestExample, MessageExample}

class JsonRpcRequestSuite extends FunSuite with Matchers {
  final val messageEx: Message = MessageExample.MESSAGE
  final val messageLao: Message = MessageExample.MESSAGE_CREATELAO_SIMPLIFIED
  private final val laoId: String = "abcd"
  private final val channelEx: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + laoId)
  private final val params: Params = new Params(channelEx)
  private final val paramsWithMessage: ParamsWithMessage = new ParamsWithMessage(channelEx, messageEx)
  private final val rpc: String = "rpc"
  private final val id: Option[Int] = Some(0)
  private final val methodType: MethodType.MethodType = MethodType.BROADCAST
  private final val rpcReq: JsonRpcRequest = JsonRpcRequest(rpc, methodType, params, id)
  private final val rpcReq2: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithMessage, id)
  private final val paramsWithMessageAndDecoded: ParamsWithMessage = new ParamsWithMessage(channelEx, messageLao)
  private final val rpcReq3: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithMessageAndDecoded, id)

  test("Constructor works for regular Params and ParamsWithMessage") {

    rpcReq.jsonrpc should equal(rpc)
    rpcReq.method should equal(methodType)
    rpcReq.params should equal(params)
    rpcReq.id should equal(id)

    rpcReq2.params should equal(paramsWithMessage)
  }

  test("getParams returns right value") {

    rpcReq.getParams should equal(params)

    rpcReq2.getParams should equal(paramsWithMessage)
  }

  test("getParamsChannel returns right value") {

    rpcReq.getParamsChannel should equal(channelEx)

    rpcReq2.getParamsChannel should equal(channelEx)
  }

  test("hasParamsMessage returns right value") {

    rpcReq.hasParamsMessage should equal(false)

    rpcReq2.hasParamsMessage should equal(true)
  }

  test("getParamsMessage returns right value") {

    rpcReq.getParamsMessage should equal(None)

    rpcReq2.getParamsMessage should equal(Some(messageEx))
  }

  test("getEncodedData returns right value") {

    rpcReq.getEncodedData should equal(None)

    rpcReq2.getEncodedData should equal(Some(Base64Data("eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ==")))
  }

  test("getDecodedData returns right value") {

    rpcReq.getDecodedData should equal(None)

    rpcReq2.getDecodedData should equal(None)

    rpcReq3.getDecodedData should equal(Some(CreateLao(Hash(Base64Data("aWQ=")), "LAO", MessageExample.NOT_STALE_TIMESTAMP, PublicKey(Base64Data("a2V5")), List.empty)))
  }

  test("getDecodedDataHeader returns right value") {

    rpcReq.getDecodedDataHeader should equal((ObjectType.INVALID, ActionType.INVALID))

    rpcReq2.getDecodedDataHeader should equal((ObjectType.INVALID, ActionType.INVALID))

    rpcReq3.getDecodedDataHeader should equal((ObjectType.LAO, ActionType.CREATE))
  }

  test("getWithDecodedData sets data as intended") {
    val messageToModify: Message = Message(messageEx.data, messageEx.sender, messageEx.signature, messageEx.message_id, messageEx.witness_signatures)
    val paramsWithMessageToModify: ParamsWithMessage = new ParamsWithMessage(channelEx, messageToModify)
    val decodedData: MessageData = CreateLao(Hash(Base64Data("aWQ=")), "LAO", Timestamp(0), PublicKey(Base64Data("a2V5")), List.empty)

    val rpcReqSet: JsonRpcRequest = JsonRpcRequest(rpc, methodType, params, id)
    val rpcReqWithParams = rpcReqSet.getWithDecodedData(decodedData)

    rpcReqWithParams should be(None)
    rpcReqSet.getDecodedData should equal(None)

    val rpcReqSet2: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithMessageToModify, id)
    val rpcReqWithParams2 = rpcReqSet2.getWithDecodedData(decodedData)

    rpcReqWithParams2 should be(defined)
    rpcReqSet2.getDecodedData should equal(None)
    rpcReqWithParams2.get.getDecodedData should equal(Some(decodedData))
  }

  test("extractLaoId returns the correct lao id") {
    rpcReq.extractLaoId should equal(Hash(Base64Data(laoId)))
  }

  test("getId returns the correct rpc id") {
    rpcReq.getId should equal(id)
    rpcReq2.getId should equal(id)
    rpcReq3.getId should equal(id)

    // Rpc request without any id (e.g. faulty message)
    JsonRpcRequest(rpc, methodType, params, None).getId should equal(None)

    // Broadcast rpc message
    JsonRpcRequestExample.broadcastRpcRequest.getId should equal(None)
  }

}
