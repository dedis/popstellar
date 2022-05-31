package ch.epfl.pop.model.network.method.message.data.election

import ch.epfl.pop.model.network.Matching

object VersionType extends Enumeration {
  type VersionType = Value

  // uninitialized placeholder
  val INVALID: Value = MatchingValue("__INVALID_OBJECT__")

  val OPEN_BALLOT: Value with Matching = MatchingValue("OPEN_BALLOT")
  val SECRET_BALLOT: Value with Matching = MatchingValue("SECRET_BALLOT")

  def MatchingValue(v: String): Value with Matching = new Val(nextId, v) with Matching

  def unapply(s: String): Option[Value] = values.find(s == _.toString)
}

