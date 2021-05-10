package ch.epfl.pop.model.objects

case class PublicKey(base64Data: Base64Data) {
  def getBytes: Array[Byte] = base64Data.getBytes
}
