package ch.epfl.pop.model.objects

import ch.epfl.dedis.lib.crypto.{Ed25519Point, Ed25519Scalar, Point}

import java.util.Random

final case class PublicKey(base64Data: Base64Data) {

  def elGamalEncrypt(messageB64: Base64Data): (Base64Data, EphemeralPublicKey) = {
    // todo : complete with
    //  https://github.com/dedis/kyber/blob/master/examples/enc_test.go
    // todo : remainders ?
    val messageBytes = messageB64.decode()
    val M = Ed25519Point.embed(messageBytes, new Random())
    // k := group.Scalar().Pick(random.New()) // ephemeral private key
    val k: Ed25519Scalar = ???
    // K = group.Point().Mul(k, nil)          // ephemeral DH public key
    val K: Point = Ed25519Point.base().mul(k)
    val S = new Ed25519Point(base64Data.decode()).mul(k)
    val C = S.add(M)
    (Base64Data.encode(C.data()), EphemeralPublicKey(K))
  }

  def equals(that: PublicKey): Boolean = base64Data == that.base64Data
}
