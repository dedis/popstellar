package ch.epfl.pop.json

import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects._
import org.scalatest.{FunSuite, Matchers}
import org.scalatest.Inspectors.{forAll,forEvery}
import org.scalatest.Matchers._



class HighLevelProtocolSuite extends FunSuite with Matchers  {

  
  test("Parser correctly encodes/decodes a Message object (1)") {
    val source: String = """{
                                 |      "message_id": "f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE=",
                                 |      "sender": "to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic=",
                                 |      "signature": "2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA==",
                                 |      "data": "eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ==",
                                 |      "witness_signatures": [{"witness": "wit1", "signature": "sig1"}, {"witness": "wit2", "signature": "sig2"}]
                                 |    }""".stripMargin


    val expected: Message = Message(
      Base64Data("eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ=="),
      PublicKey(Base64Data("to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic=")),
      Signature(Base64Data("2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA==")),
      Hash(Base64Data("f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE=")),
      WitnessSignaturePair(PublicKey(Base64Data("wit1")), Signature(Base64Data("sig1"))) :: WitnessSignaturePair(PublicKey(Base64Data("wit2")), Signature(Base64Data("sig2"))) :: Nil
    )

    val message = Message.buildFromJson(source)

    message shouldBe a [Message]
    message should equal (expected)
  }
test("Parser: Empty json") {
    val source: String = """{
                                 |      "message_id": "",
                                 |      "sender": "",
                                 |      "signature": "",
                                 |      "data": "",
                                 |      "witness_signatures": []
                                 |    }""".stripMargin


    val expected: Message = Message(
      Base64Data(""),
      PublicKey(Base64Data("")),
      Signature(Base64Data("")),
      Hash(Base64Data("")),
      Nil
    )

    val message = Message.buildFromJson(source)

    message shouldBe a [Message]
    message should equal (expected)
  }
  test("Parser: Empty message_id array") {
    val source: String = """{
                                 |      "message_id": "",
                                 |      "sender": "to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic=",
                                 |      "signature": "2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA==",
                                 |      "data": "eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ==",
                                 |      "witness_signatures": []
                                 |    }""".stripMargin


    val expected: Message = Message(
      Base64Data("eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ=="),
      PublicKey(Base64Data("to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic=")),
      Signature(Base64Data("2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA==")),
      Hash(Base64Data("")),
      Nil
    )

    val message = Message.buildFromJson(source)

    message shouldBe a [Message]
    message should equal (expected)
  }

  test("Parser: Empty message_id and sender pk array") {
    val source: String = """{
                                 |      "message_id": "",
                                 |      "sender": "",
                                 |      "signature": "2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA==",
                                 |      "data": "eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ==",
                                 |      "witness_signatures": []
                                 |    }""".stripMargin


    val expected: Message = Message(
      Base64Data("eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ=="),
      PublicKey(Base64Data("")),
      Signature(Base64Data("2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA==")),
      Hash(Base64Data("")),
      Nil
    )

    val message = Message.buildFromJson(source)

    message shouldBe a [Message]
    message should equal (expected)
  }

  test("Parser: Empty witness array") {
    val source: String = """{
                                 |      "message_id": "f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE=",
                                 |      "sender": "to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic=",
                                 |      "signature": "2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA==",
                                 |      "data": "eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ==",
                                 |      "witness_signatures": []
                                 |    }""".stripMargin


    val expected: Message = Message(
      Base64Data("eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ=="),
      PublicKey(Base64Data("to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic=")),
      Signature(Base64Data("2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA==")),
      Hash(Base64Data("f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE=")),
      Nil
    )

    val message = Message.buildFromJson(source)

    message shouldBe a [Message]
    message should equal (expected)
  }


  test("Parser: Empty source") {
    val source: String = ""
    an [spray.json.JsonParser.ParsingException] should be thrownBy(Message.buildFromJson(source))
  }
 
  test("Parser: Null source ") {
    val source: String = """{
                                 |      "message_id": null.
                                 |      "sender": null,
                                 |      "signature": null,
                                 |      "data": null,
                                 |      "witness_signatures": null
                                 |    }""".stripMargin

    an [spray.json.JsonParser.ParsingException] should be thrownBy(Message.buildFromJson(source))
  }
  
  test("Invalid json keys format test without witness sigs"){
  
    forEvery (Seq( "[]", "{}", "null")) {
        (t: String) =>  {
          val source: String = f"""{
                                 |      "message_id": $t,
                                 |      "sender": $t,
                                 |      "signature": $t,
                                 |      "data": $t,
                                 |      "witness_signatures": []
                                 |    }""".stripMargin

             
        an [IllegalArgumentException] should be thrownBy(Message.buildFromJson(source))
        
    }
  }
}


}

