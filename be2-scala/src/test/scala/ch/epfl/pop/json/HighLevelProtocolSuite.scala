package ch.epfl.pop.json

import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects._
import org.scalatest.{FunSuite, Matchers}
import org.scalatest.Inspectors.{forAll,forEvery}
import org.scalatest.Matchers._
import ch.epfl.pop.json.MsgField._
import org.scalactic.Prettifier

class HighLevelProtocolSuite extends FunSuite with Matchers  {

    def buildExpected(id: String, sender: String, signature: String, data: String): Message ={
      Message(
        Base64Data(data),
        PublicKey(Base64Data(sender)),
        Signature(Base64Data(signature)),
        Hash(Base64Data(id)),
        Nil //Witnesses will be added in each test that needs them
      )
    }

    def getFormattedString(f: MsgField, sp: String): String = {
      val format =  s"""{
                                 |      "message_id": "%s",
                                 |      "sender": "%s",
                                 |      "signature": "%s",
                                 |      "data": "%s",
                                 |      "witness_signatures": "%s"
                                 |    }""".stripMargin

      f match {
          case MESSAGE_ID => String.format(format, sp, "", "", "" ,"[]")
          case SENDER => String.format(format, "", sp, "", "" ,"[]")
          case SIGNATURE => String.format(format, "", "", sp, "" ,"[]")
          case DATA => String.format(format, "", "", "", sp ,"[]")
          case WITNESS_SIGNATURES => String.format(format, "", "", "", "" ,sp)
      }
  }

  test("Parser correctly parse to Message object") {
    val id: String = "f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE="
    val sender: String ="to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic="
    val signature: String = "2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA=="
    val data: String = "eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ=="
    val source: String = s"""{
                                 |      "message_id": "$id",
                                 |      "sender": "$sender",
                                 |      "signature": "$signature",
                                 |      "data": "$data",
                                 |      "witness_signatures": [{"witness": "wit1", "signature": "sig1"}, {"witness": "wit2", "signature": "sig2"}]
                                 |    }""".stripMargin

    var expected: Message = buildExpected(id, sender, signature, data)
    // Add witnesses to the expected message
    val witness = (WitnessSignaturePair(PublicKey(Base64Data("wit1")), Signature(Base64Data("sig1")))
      :: WitnessSignaturePair(PublicKey(Base64Data("wit2")), Signature(Base64Data("sig2")))
      :: Nil).reverse.foreach(w => {expected = expected.addWitnessSignature(w)})

    val message = Message.buildFromJson(source)

    message shouldBe a [Message]
    message should equal (expected)
  }

  test("Parser: Empty json") {
    val id: String = ""
    val sender: String = ""
    val signature: String = ""
    val data: String = ""
    val source: String =  s"""{
                               |      "message_id": "$id",
                               |      "sender": "$sender",
                               |      "signature": "$signature",
                               |      "data": "$data",
                               |      "witness_signatures": []
                               |    }""".stripMargin


    var expected: Message = buildExpected(id, sender, signature, data)
    val message = Message.buildFromJson(source)

    message shouldBe a [Message]
    message should equal (expected)
  }

  test("Parser: Empty witness array") {
     val id: String = "f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE="
    val sender: String ="to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic="
    val signature: String = "2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA=="
    val data: String = "eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ=="
    val source: String = s"""{
                                 |      "message_id": "$id",
                                 |      "sender": "$sender",
                                 |      "signature": "$signature",
                                 |      "data": "$data",
                                 |      "witness_signatures": []
                                 |    }""".stripMargin

    val expected: Message = buildExpected(id, sender, signature, data)
    val message = Message.buildFromJson(source)
    message shouldBe a [Message]
    message should equal (expected)
  }

  test("Parser: Empty source") {
    val source: String = ""
    an [spray.json.JsonParser.ParsingException] should be thrownBy(Message.buildFromJson(source))
  }

  test("Invalid json keys format test"){
  val set = for { p <- (MsgField.values - WITNESS_SIGNATURES)
                  t <- Set("[]", "{}", "null", "1")
                } yield (t,p)

  val setWitness = Set("","1", "{}", "null").map((_,WITNESS_SIGNATURES))
  implicit val prettifier = Prettifier.default
  forEvery(set union setWitness){
    case (t: String, p: MsgField) =>  {
        val source: String = getFormattedString(p,t)
        an [IllegalArgumentException] should be thrownBy(Message.buildFromJson(source))
      }
    }
  }
}
