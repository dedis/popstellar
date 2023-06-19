package ch.epfl.pop.model.network.method.message.data.popcha

import ch.epfl.pop.json.MessageDataProtocol.AuthenticateDataFormat
import ch.epfl.pop.model.network.method.message.data.popcha.AuthenticateSuite.AUTHENTICATE_MESSAGE
import ch.epfl.pop.model.objects.{Base64Data, PrivateKey, PublicKey, Signature}
import org.scalatest.funsuite.{AnyFunSuite => FunSuite}
import org.scalatest.matchers.should.Matchers
import spray.json.enrichAny

class AuthenticateSuite extends FunSuite with Matchers {
  test("json encoding / decoding keeps the object intact") {
    val encodedDecoded = Authenticate.buildFromJson(AUTHENTICATE_MESSAGE.toJson.toString())
    encodedDecoded shouldBe AUTHENTICATE_MESSAGE
  }
}

object AuthenticateSuite {
  val NONCE = "I am unique"
  val CLIENT_ID = "Some id"

  val KEY_DATA: Base64Data = Base64Data.encode("Some long term identifier abcdef")
  val IDENTIFIER: PublicKey = PublicKey(KEY_DATA)
  val IDENTIFIER_PROOF: Signature = PrivateKey(KEY_DATA).signData(Base64Data.encode(NONCE))

  val STATE = "Some optional state"
  val RESPONSE_MODE = "query"
  val POPCHA_ADDRESS = "https://server.com"

  val AUTHENTICATE_MESSAGE: Authenticate = Authenticate(CLIENT_ID, NONCE, IDENTIFIER, IDENTIFIER_PROOF, STATE, RESPONSE_MODE, POPCHA_ADDRESS)
}
