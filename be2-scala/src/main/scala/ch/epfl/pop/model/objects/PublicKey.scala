package ch.epfl.pop.model.objects

import ch.epfl.dedis.lib.crypto._

final case class PublicKey(base64Data: Base64Data) {
  val point = new Ed25519Point(base64Data.data)

  def equals(that: PublicKey): Boolean = base64Data == that.base64Data
}
