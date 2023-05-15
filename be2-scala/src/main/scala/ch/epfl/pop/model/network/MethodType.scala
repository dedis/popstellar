package ch.epfl.pop.model.network

object MethodType extends Enumeration {
  type MethodType = Value

  // uninitialized placeholder
  val INVALID: Value = MatchingValue("__INVALID_METHOD__")

  val BROADCAST: Value = MatchingValue("broadcast")
  val PUBLISH: Value = MatchingValue("publish")
  val SUBSCRIBE: Value = MatchingValue("subscribe")
  val UNSUBSCRIBE: Value = MatchingValue("unsubscribe")
  val CATCHUP: Value = MatchingValue("catchup")

  val HEARTBEAT: Value = MatchingValue("heartbeat")
  val GET_MESSAGES_BY_ID: Value = MatchingValue("get_messages_by_id")

  val GREET_SERVER: Value = MatchingValue("greet_server")
  def MatchingValue(v: String): Value with Matching = new Val(nextId, v) with Matching

  def unapply(s: String): Option[Value] = values.find(s == _.toString)
}
