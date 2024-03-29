package ch.epfl.pop.json

import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.{GreetServer, ParamsWithChannel, ParamsWithMap, ParamsWithMessage}
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse, MethodType, ResultObject}
import ch.epfl.pop.model.objects.*
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import org.scalatest.Inspectors.forEvery
import org.scalatest.funsuite.AnyFunSuite as FunSuite
import org.scalatest.matchers.should.Matchers
import spray.json.*

import scala.collection.immutable.{HashMap, Set}

class HighLevelProtocolSuite extends FunSuite with Matchers {

  import MsgField._

  def buildExpected(id: String, sender: String, signature: String, data: String): Message = {
    Message(
      Base64Data(data),
      PublicKey(Base64Data(sender)),
      Signature(Base64Data(signature)),
      Hash(Base64Data(id)),
      Nil // Witnesses will be added in each test that needs them
    )
  }

  def getFormattedString(f: MsgField, sp: String): String = {
    val format =
      s"""{
         |      "message_id": "%s",
         |      "sender": "%s",
         |      "signature": "%s",
         |      "data": "%s",
         |      "witness_signatures": "%s"
         |    }""".stripMargin

    f match {
      case MESSAGE_ID         => String.format(format, sp, "", "", "", "[]")
      case SENDER             => String.format(format, "", sp, "", "", "[]")
      case SIGNATURE          => String.format(format, "", "", sp, "", "[]")
      case DATA               => String.format(format, "", "", "", sp, "[]")
      case WITNESS_SIGNATURES => String.format(format, "", "", "", "", sp)
    }
  }

  def getFormattedSource(id: String, sender: String, signature: String, data: String, witnesses: String): String = {
    s"""{
       |      "message_id": "$id",
       |      "sender": "$sender",
       |      "signature": "$signature",
       |      "data": "$data",
       |      "witness_signatures": $witnesses
       |    }""".stripMargin
  }

  test("Parser correctly parse to Message object") {
    val id: String = "f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE="
    val sender: String = "to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic="
    val signature: String = "2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA=="
    val data: String = "eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ=="
    val witnessesSigs: String = "[{\"witness\": \"wit1\", \"signature\": \"sig1\"}, {\"witness\": \"wit2\", \"signature\": \"sig2\"}]"

    val source = getFormattedSource(id, sender, signature, data, witnessesSigs)
    var expected: Message = buildExpected(id, sender, signature, data)
    // Add witnesses to the expected message
    val witness: Unit = (WitnessSignaturePair(PublicKey(Base64Data("wit1")), Signature(Base64Data("sig1")))
      :: WitnessSignaturePair(PublicKey(Base64Data("wit2")), Signature(Base64Data("sig2")))
      :: Nil).reverse.foreach(w => {
      expected = expected.addWitnessSignature(w)
    })

    val message = Message.buildFromJson(source)

    message shouldBe a[Message]
    message should equal(expected)
  }

  test("Parser: Empty json") {
    val id: String = ""
    val sender: String = ""
    val signature: String = ""
    val data: String = ""
    val witnessesSigs: String = "[]"
    val source = getFormattedSource(id, sender, signature, data, witnessesSigs)

    val expected: Message = buildExpected(id, sender, signature, data)
    val message = Message.buildFromJson(source)

    message shouldBe a[Message]
    message should equal(expected)
  }

  test("Parser: Empty witness array") {
    val id: String = "f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE="
    val sender: String = "to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic="
    val signature: String = "2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA=="
    val data: String = "eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ=="
    val witnessesSigs: String = "[]"

    val source = getFormattedSource(id, sender, signature, data, witnessesSigs)

    val expected: Message = buildExpected(id, sender, signature, data)
    val message = Message.buildFromJson(source)
    message shouldBe a[Message]
    message should equal(expected)
  }

  test("Parser: Empty source") {
    val source: String = ""
    an[spray.json.JsonParser.ParsingException] should be thrownBy (Message.buildFromJson(source))
  }

  /** This tests against all not allowed key types/formats
    */
  test("Invalid json keys format test") {
    // Forms pairs/combinations of different MsgField values (except Witness) and forbidden key types
    // for formating
    val set = for {
      p <- MsgField.values.toSet - WITNESS_SIGNATURES
      t <- Set("[]", "{}", "null", "1")
    } yield (t, p)

    // Form left pairs/combinations of witness signatures and its forbidden key types for formatting
    val setWitness = Set("", "1", "{}", "null").map((_, WITNESS_SIGNATURES))

    // Test against all the (incorrect) combinations
    forEvery(set union setWitness) {
      case (t: String, p: MsgField) =>
        val source: String = getFormattedString(p, t)
        an[IllegalArgumentException] should be thrownBy Message.buildFromJson(source)
    }
  }

  test("parse correctly heartbeat") {

    // Setup
    val chan1 = Channel("/root/nLghr9_P406lfkMjaNWqyohLxOiGlQee8zad4qAfj18=/social/8qlv4aUT5-tBodKp4RszY284CFYVaoDZK6XKiw9isSw=")
    val id1 = Hash(Base64Data("DCBX48EuNO6q-Sr42ONqsj7opKiNeXyRzrjqTbZ_aMI="))

    val chan2 = Channel("/root/nLghr9_P406lfkMjaNWqyohLxOiGlQee8zad4qAfj18=/HnXDyvSSron676Icmvcjk5zXvGLkPJ1fVOaWOxItzBE=")
    val id2 = Hash(Base64Data("z6SbjJ0Hw36k8L09-GVRq4PNmi06yQX4e8aZRSbUDwc="))
    val id3 = Hash(Base64Data("txbTmVMwCDkZdoaAiEYfAKozVizZzkeMkeOlzq5qMlg="))
    val map = HashMap(
      chan1 -> Set(id1),
      chan2 -> Set(id2, id3)
    )

    val hbJsValue = HighLevelProtocol.jsonRpcRequestFormat.write(JsonRpcRequest(RpcValidator.JSON_RPC_VERSION, MethodType.heartbeat, new ParamsWithMap(map), None))
    val hbFromJson = JsonRpcRequest.buildFromJson(hbJsValue.prettyPrint)

    // Test
    hbFromJson.jsonrpc should equal(RpcValidator.JSON_RPC_VERSION)
    hbFromJson.method should equal(MethodType.heartbeat)
    hbFromJson.getParams.asInstanceOf[ParamsWithMap].channelsToMessageIds should equal(map)
    hbFromJson.id should equal(None)

  }

  test("parse correctly get_messages_by_id") {

    // Setup
    val chan1 = Channel("/root/nLghr9_P406lfkMjaNWqyohLxOiGlQee8zad4qAfj18=/social/8qlv4aUT5-tBodKp4RszY284CFYVaoDZK6XKiw9isSw=")
    val id1 = Hash(Base64Data("DCBX48EuNO6q-Sr42ONqsj7opKiNeXyRzrjqTbZ_aMI="))

    val chan2 = Channel("/root/nLghr9_P406lfkMjaNWqyohLxOiGlQee8zad4qAfj18=/HnXDyvSSron676Icmvcjk5zXvGLkPJ1fVOaWOxItzBE=")
    val id2 = Hash(Base64Data("z6SbjJ0Hw36k8L09-GVRq4PNmi06yQX4e8aZRSbUDwc="))
    val id3 = Hash(Base64Data("txbTmVMwCDkZdoaAiEYfAKozVizZzkeMkeOlzq5qMlg="))
    val map = HashMap(
      chan1 -> Set(id1),
      chan2 -> Set(id2, id3)
    )

    val id = Some(5)
    val getMsgsByIdJsValue = HighLevelProtocol.jsonRpcRequestFormat.write(JsonRpcRequest(RpcValidator.JSON_RPC_VERSION, MethodType.get_messages_by_id, new ParamsWithMap(map), id))
    val getMsgsByIdFromJson = JsonRpcRequest.buildFromJson(getMsgsByIdJsValue.prettyPrint)

    // Test
    getMsgsByIdFromJson.jsonrpc should equal(RpcValidator.JSON_RPC_VERSION)
    getMsgsByIdFromJson.method should equal(MethodType.get_messages_by_id)
    getMsgsByIdFromJson.getParams.asInstanceOf[ParamsWithMap].channelsToMessageIds should equal(map)
    getMsgsByIdFromJson.id should equal(id)
  }

  test("parse correctly greetServer") {
    val pk: PublicKey = PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM="))
    val clientAddress: String = "wss://popdemo.dedis.ch:9000/client"
    val serverAddress: String = "wss://popdemo.dedis.ch:9001/server"

    val greetServerJsValue = HighLevelProtocol.jsonRpcRequestFormat.write(JsonRpcRequest(RpcValidator.JSON_RPC_VERSION, MethodType.greet_server, new GreetServer(pk, clientAddress, serverAddress), None))
    val greetServerFromJson = JsonRpcRequest.buildFromJson(greetServerJsValue.prettyPrint)

    // Test
    greetServerFromJson.jsonrpc should equal(RpcValidator.JSON_RPC_VERSION)
    greetServerFromJson.method should equal(MethodType.greet_server)
    greetServerFromJson.getParams.asInstanceOf[GreetServer].clientAddress should equal(clientAddress)
    greetServerFromJson.getParams.asInstanceOf[GreetServer].serverAddress should equal(serverAddress)
    greetServerFromJson.getParams.asInstanceOf[GreetServer].publicKey should equal(pk)

  }

  test("parse correctly catchup") {

    val chan1 = Channel("/root/nLghr9_P406lfkMjaNWqyohLxOiGlQee8zad4qAfj18=/social/8qlv4aUT5-tBodKp4RszY284CFYVaoDZK6XKiw9isSw=")
    val id = Some(5)

    val catchupJsValue = HighLevelProtocol.jsonRpcRequestFormat.write(JsonRpcRequest(RpcValidator.JSON_RPC_VERSION, MethodType.catchup, new ParamsWithChannel(chan1), id))
    val catchupFromJson = JsonRpcRequest.buildFromJson(catchupJsValue.prettyPrint)

    // Test
    catchupFromJson.jsonrpc should equal(RpcValidator.JSON_RPC_VERSION)
    catchupFromJson.method should equal(MethodType.catchup)
    catchupFromJson.getParams.asInstanceOf[ParamsWithChannel].channel should equal(chan1)
    catchupFromJson.id should equal(id)
  }

  test("parse correctly broadcast") {

    // Setup
    val chan1 = Channel("/root/nLghr9_P406lfkMjaNWqyohLxOiGlQee8zad4qAfj18=/social/8qlv4aUT5-tBodKp4RszY284CFYVaoDZK6XKiw9isSw=")

    val id: String = "f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE="
    val sender: String = "to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic="
    val signature: String = "2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA=="
    val data: String = "eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ=="
    val message: Message = buildExpected(id, sender, signature, data)

    val broadcastJsValue = HighLevelProtocol.jsonRpcRequestFormat.write(JsonRpcRequest(RpcValidator.JSON_RPC_VERSION, MethodType.broadcast, new ParamsWithMessage(chan1, message), None))
    val broadcastFromJson = JsonRpcRequest.buildFromJson(broadcastJsValue.prettyPrint)

    // Test
    broadcastFromJson.jsonrpc should equal(RpcValidator.JSON_RPC_VERSION)
    broadcastFromJson.method should equal(MethodType.broadcast)
    broadcastFromJson.getParams.asInstanceOf[ParamsWithMessage].channel should equal(chan1)
    broadcastFromJson.getParams.asInstanceOf[ParamsWithMessage].message should equal(message)
    broadcastFromJson.id should equal(None)
  }

  test("parse correctly get_messages_by_id answers") {

    val chan1 = Channel("/root/nLghr9_P406lfkMjaNWqyohLxOiGlQee8zad4qAfj18=/social/8qlv4aUT5-tBodKp4RszY284CFYVaoDZK6XKiw9isSw=")

    val id: String = "f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE="
    val sender: String = "to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic="
    val signature: String = "2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA=="
    val data: String = "eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ=="
    val message1: Message = buildExpected(id, sender, signature, data)

    val resultObject = new ResultObject(Map((chan1, Set(message1))))
    val rpcId = Some(2)

    val answerJsValue = HighLevelProtocol.jsonRpcResponseFormat.write(JsonRpcResponse(RpcValidator.JSON_RPC_VERSION, resultObject, rpcId))
    val answerFromJson = JsonRpcResponse.buildFromJson(answerJsValue.prettyPrint)

    // Test
    answerFromJson.jsonrpc should equal(RpcValidator.JSON_RPC_VERSION)
    answerFromJson.id should equal(rpcId)
    answerFromJson.result.get should equal(resultObject)
    answerFromJson.error should equal(None)
  }

  test("Parser correctly encodes and decodes MethodType and rejects incorrect type") {
    MethodType.values.foreach(obj => {
      val fromJson = HighLevelProtocol.methodTypeFormat.write(obj)
      val toType = HighLevelProtocol.methodTypeFormat.read(fromJson)
      toType shouldBe a[MethodType]
    })
    val invalidJson = """{"method": "stellarmethod"}""".parseJson
    assertThrows[IllegalArgumentException] {
      HighLevelProtocol.methodTypeFormat.read(invalidJson)
    }
  }

  test("Parser correctly encodes and decodes publish, subscribe, get_messages_by_id and rejects INVALID type") {
    //Test focused on new MethodType conversion
    // Setup
    val chan1 = Channel("/root/nLghr9_P406lfkMjaNWqyohLxOiGlQee8zad4qAfj18=/social/8qlv4aUT5-tBodKp4RszY284CFYVaoDZK6XKiw9isSw=")

    val id: String = "f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE="
    val sender: String = "to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic="
    val signature: String = "2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA=="
    val data: String = "eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ=="
    val message: Message = buildExpected(id, sender, signature, data)


    val id1 = Hash(Base64Data("DCBX48EuNO6q-Sr42ONqsj7opKiNeXyRzrjqTbZ_aMI="))

    val chan2 = Channel("/root/nLghr9_P406lfkMjaNWqyohLxOiGlQee8zad4qAfj18=/HnXDyvSSron676Icmvcjk5zXvGLkPJ1fVOaWOxItzBE=")
    val id2 = Hash(Base64Data("z6SbjJ0Hw36k8L09-GVRq4PNmi06yQX4e8aZRSbUDwc="))
    val id3 = Hash(Base64Data("txbTmVMwCDkZdoaAiEYfAKozVizZzkeMkeOlzq5qMlg="))
    val map = HashMap(
      chan1 -> Set(id1),
      chan2 -> Set(id2, id3)
    )

    val methodTypesToTest = List(MethodType.publish, MethodType.subscribe, MethodType.get_messages_by_id, MethodType.INVALID)
    //Other types are already covered in other tests, since it's simply converting to and from json there's no need to test twice
    //The more interesting part here is testing for invalid types
    methodTypesToTest.foreach(obj => {
      val fromJson = HighLevelProtocol.jsonRpcRequestFormat.write(
        JsonRpcRequest(RpcValidator.JSON_RPC_VERSION, obj, if obj == MethodType.get_messages_by_id then new ParamsWithMap(map) else new ParamsWithMessage(chan1, message),
          if obj == MethodType.greet_server then None else Some(23)
        )
      )

      if obj == MethodType.INVALID then
        assertThrows[IllegalArgumentException] {HighLevelProtocol.jsonRpcRequestFormat.read(fromJson)}
      else
        val toType = HighLevelProtocol.jsonRpcRequestFormat.read(fromJson)
        toType shouldBe a[JsonRpcRequest]
        toType.method should equal(obj)
    })
  }


}

enum MsgField:
  case MESSAGE_ID, SENDER, SIGNATURE, DATA, WITNESS_SIGNATURES
