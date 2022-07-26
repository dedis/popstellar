package ch.epfl.pop.model.objects

import org.scalatest.{FunSuite, Matchers}
import spray.json._

class PublicKeySuite extends FunSuite with Matchers {
  private val data = {
    val source = scala.io.Source.fromFile("../tests/data/keypair.json")
    val payload =
      try source.mkString
      finally source.close()

    import DefaultJsonProtocol._
    payload.parseJson
  }

  test("pubkey hash matches example data") {
    for (suffix <- Seq("", "2")) {
      val Seq(JsString(pubkeyData), JsString(pubkeyHash)) = data.asJsObject.getFields(s"publicKey$suffix", s"publicKeyHash$suffix")
      val pubkey = PublicKey(Base64Data(pubkeyData))
      pubkey.hash.base64Data should equal(Base64Data(pubkeyHash))
    }
  }
}
