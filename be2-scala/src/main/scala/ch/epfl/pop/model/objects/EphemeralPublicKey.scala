package ch.epfl.pop.model.objects

import ch.epfl.dedis.lib.crypto.{Ed25519Point, Point}


final case class EphemeralPublicKey(keyB64: Base64Data) {
  val asPoint: Ed25519Point = new Ed25519Point(keyB64.decode())
}

object EphemeralPublicKey {
  def apply(key: Point): EphemeralPublicKey = new EphemeralPublicKey(Base64Data.encode(key.data()))
}
