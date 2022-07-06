package ch.epfl.pop.json

import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects._
import org.scalatest.Inspectors.forEvery
import org.scalatest.{FunSuite, Matchers}

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
      p <- (MsgField.values - WITNESS_SIGNATURES)
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
}

object MsgField extends Enumeration {
  type MsgField = Value
  val MESSAGE_ID, SENDER, SIGNATURE, DATA, WITNESS_SIGNATURES = Value
}
