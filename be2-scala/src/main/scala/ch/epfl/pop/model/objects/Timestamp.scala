package ch.epfl.pop.model.objects

final case class Timestamp(time: Long) {
  def <(other: Long): Boolean = time < other

  def <(other: Timestamp): Boolean = time < other.time

  def <=(other: Long): Boolean = time <= other

  def <=(other: Timestamp): Boolean = time <= other.time

  override def toString: String = time.toString
}
