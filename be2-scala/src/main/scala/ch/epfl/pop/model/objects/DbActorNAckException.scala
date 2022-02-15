package ch.epfl.pop.model.objects

case class DbActorNAckException(code: Int, message: String) extends Exception(message) {

  def this(code: Int, message: String, cause: Throwable) = {
    this(code, message)
    initCause(cause)
  }
}
