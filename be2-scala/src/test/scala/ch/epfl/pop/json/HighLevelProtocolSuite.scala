package ch.epfl.pop.json

import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects._
import org.scalatest.{FunSuite, Matchers}

class HighLevelProtocolSuite extends FunSuite with Matchers {

  test("Parser correctly encodes/decodes a Message object") {
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
}
