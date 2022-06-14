package ch.epfl.pop.model.network.method.message.data

import ch.epfl.pop.model.network.Matching

object ActionType extends Enumeration {
  type ActionType = Value

  // uninitialized placeholder
  val INVALID: Value = MatchingValue("__INVALID_ACTION__")

  val CREATE: Value = MatchingValue("create")
  val UPDATE_PROPERTIES: Value = MatchingValue("update_properties")
  val STATE: Value = MatchingValue("state")
  val GREET: Value = MatchingValue("greet")
  val WITNESS: Value = MatchingValue("witness")
  val OPEN: Value = MatchingValue("open")
  val REOPEN: Value = MatchingValue("reopen")
  val CLOSE: Value = MatchingValue("close")
  // election actions:
  val SETUP: Value = MatchingValue("setup")
  val RESULT: Value = MatchingValue("result")
  val END: Value = MatchingValue("end")
  val CAST_VOTE: Value = MatchingValue("cast_vote")
  val KEY: Value = MatchingValue("key")
  // social media actions:
  val ADD: Value = MatchingValue("add")
  val DELETE: Value = MatchingValue("delete")
  val NOTIFY_ADD: Value = MatchingValue("notify_add")
  val NOTIFY_DELETE: Value = MatchingValue("notify_delete")
  // digital cash actions:
  val POST_TRANSACTION: Value = MatchingValue("post_transaction")

  def MatchingValue(v: String): Value with Matching = new Val(nextId, v) with Matching

  def unapply(s: String): Option[Value] = values.find(s == _.toString)
}
