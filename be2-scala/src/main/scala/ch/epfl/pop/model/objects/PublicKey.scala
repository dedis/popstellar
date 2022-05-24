package ch.epfl.pop.model.objects

import ch.epfl.dedis.lib.crypto.{Ed25519Point, Ed25519Scalar}

import java.util.Random

final case class PublicKey(base64Data: Base64Data) {

  def elGamalEncrypt(messageB64: Base64Data): Base64Data = {
    // todo : complete with
    //  https://github.com/dedis/kyber/blob/master/examples/enc_test.go
    val messageBytes = messageB64.decode()
    val M = Ed25519Point.embed(messageBytes, new Random())
    val max: Int = math.max(??? /*group.Point().EmbedLen() in go*/ ,
      messageB64.decode().length)
    val remainder = messageBytes.slice(max, messageBytes.length)
    val k: Ed25519Scalar = ???
    val K: Ed25519Point = ???
    val S = new Ed25519Point(base64Data.decode()).mul(k)
    val C = S.add(M)
    Base64Data.encode(C.data())
  }

  def equals(that: PublicKey): Boolean = base64Data == that.base64Data
}
