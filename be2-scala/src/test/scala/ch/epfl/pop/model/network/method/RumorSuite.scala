package ch.epfl.pop.model.network.method

import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.{Base64Data, Channel, Hash, PublicKey, Signature}
import org.scalatest.matchers.should.Matchers
import org.scalatest.funsuite.AnyFunSuite as FunSuite
import ch.epfl.pop.json.HighLevelProtocol.RumorFormat
import spray.json.enrichAny

import scala.collection.immutable.HashMap
import scala.language.postfixOps

class RumorSuite extends FunSuite with Matchers {
  private final val serverPk: PublicKey = new PublicKey(Base64Data.encode("PublickKey"))

  def buildExpected(id: String, sender: String, signature: String, data: String): Message = {
    Message(
      Base64Data(data),
      PublicKey(Base64Data(sender)),
      Signature(Base64Data(signature)),
      Hash(Base64Data(id)),
      Nil // Witnesses will be added in each test that needs them
    )
  }

  test("Constructor from Json works for Rumor") {
    val chan: Channel = Channel("/root/nLghr9_P406lfkMjaNWqyohLxOiGlQee8zad4qAfj18=/social/8qlv4aUT5-tBodKp4RszY284CFYVaoDZK6XKiw9isSw=")
    val id: String = "f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE="
    val sender: String = "to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic="
    val signature: String = "2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA=="
    val data: String = "eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ=="
    val message: Message = buildExpected(id, sender, signature, data)

    val messages: Map[Channel, List[Message]] = HashMap(
      chan -> List(message)
    )
    val rumor: Rumor = new Rumor(senderPk = serverPk, rumorId = 1, messages = messages)

    val encodedDecoded = Rumor.buildFromJson(rumor.toJson.toString)

    encodedDecoded.rumorId shouldBe rumor.rumorId
    encodedDecoded.senderPk shouldBe rumor.senderPk
    encodedDecoded.messages.values.zip(rumor.messages.values).foreach((arrMsg1, arrMsg2) => arrMsg1 shouldBe arrMsg2)
    encodedDecoded.messages.keys shouldBe rumor.messages.keys
  }

}
