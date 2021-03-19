package ch.epfl.pop.model.objects

case class Signature(signature: Base64Data) {
  def verify(key: PublicKey, message: Base64Data): Boolean = true // FIXME implement verify
}
