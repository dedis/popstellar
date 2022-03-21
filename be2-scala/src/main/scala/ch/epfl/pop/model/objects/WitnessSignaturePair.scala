package ch.epfl.pop.model.objects

final case class WitnessSignaturePair(witness: PublicKey, signature: Signature) {
  def verify(messageId: Hash): Boolean = signature.verify(witness, messageId.base64Data)
}
