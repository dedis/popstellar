package ch.epfl.pop.model.objects

final case class PublicKey(base64Data: Base64Data) {
  def equals(that: PublicKey): Boolean = base64Data == that.base64Data
}
