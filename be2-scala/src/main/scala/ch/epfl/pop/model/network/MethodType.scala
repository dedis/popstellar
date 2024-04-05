package ch.epfl.pop.model.network

enum MethodType:
  case INVALID extends MethodType
  case broadcast extends MethodType
  case publish extends MethodType
  case subscribe extends MethodType
  case unsubscribe extends MethodType
  case catchup extends MethodType
  case heartbeat extends MethodType
  case get_messages_by_id extends MethodType
  case greet_server extends MethodType
  case rumor extends MethodType

object MethodType:
  def apply(method: String): MethodType =
    method match
      case "broadcast"          => broadcast
      case "publish"            => publish
      case "subscribe"          => subscribe
      case "unsubscribe"        => unsubscribe
      case "catchup"            => catchup
      case "heartbeat"          => heartbeat
      case "get_messages_by_id" => get_messages_by_id
      case "greet_server"       => greet_server
      case "rumor"              => rumor
      case _                    => INVALID
