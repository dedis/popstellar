package util.examples.data

import ch.epfl.pop.model.objects._
import spray.json._

final case class KeyPairWithHash(keyPair: KeyPair, pubkeyHash: Base64Data)

object TestKeyPairs {
  private val raw = {
    val source = scala.io.Source.fromFile("../tests/data/keypair.json")
    val payload =
      try source.mkString
      finally source.close()

    import DefaultJsonProtocol._
    payload.parseJson.asJsObject
  }

  val keypairs: Vector[KeyPairWithHash] = for (suffix <- Vector("", "2")) yield {
    val Seq(JsString(privkeyData), JsString(pubkeyData), JsString(pubkeyHash)) =
      raw.getFields(s"privateKey$suffix", s"publicKey$suffix", s"publicKeyHash$suffix")
    val fixedPrivkeyData = Base64Data(privkeyData).decode().take(32)
    KeyPairWithHash(KeyPair(PrivateKey(Base64Data.encode(fixedPrivkeyData)), PublicKey(Base64Data(pubkeyData))), Base64Data(pubkeyHash))
  }
}
