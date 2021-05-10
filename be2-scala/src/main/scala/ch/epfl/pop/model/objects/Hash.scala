package ch.epfl.pop.model.objects

case class Hash(base64Data: Base64Data)

object Hash {
  def fromStrings(strs: String*): Hash = ???
}
