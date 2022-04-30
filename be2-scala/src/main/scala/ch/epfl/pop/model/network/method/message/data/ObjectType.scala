package ch.epfl.pop.model.network.method.message.data

import ch.epfl.pop.model.network.Matching

object ObjectType extends Enumeration {
  type ObjectType = Value

  // uninitialized placeholder
  val INVALID: Value = MatchingValue("__INVALID_OBJECT__")

  val LAO: Value with Matching = MatchingValue("lao")
  val MESSAGE: Value with Matching = MatchingValue("message")
  val MEETING: Value with Matching = MatchingValue("meeting")
  val ROLL_CALL: Value with Matching = MatchingValue("roll_call")
  val ELECTION: Value with Matching = MatchingValue("election")
  val CHIRP: Value with Matching = MatchingValue("chirp")
  val REACTION: Value with Matching = MatchingValue("reaction")
  val TRANSACTION: Value with Matching = MatchingValue("transaction")

  def MatchingValue(v: String): Value with Matching = new Val(nextId, v) with Matching

  def unapply(s: String): Option[Value] = values.find(s == _.toString)
}

