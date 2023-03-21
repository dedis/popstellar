package ch.epfl.pop.model.objects

final case class HbActorNAckException(code: Int, message: String) extends Exception(message) {

  def this(code: Int, message: String, cause: Throwable) = {
    this(code, message)
    initCause(cause)
  }
}
