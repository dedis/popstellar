package ch.epfl.pop.model.network

enum MethodType:
  case INVALID extends MethodType
  case BROADCAST extends MethodType
  case PUBLISH extends MethodType
  case SUBSCRIBE extends MethodType
  case UNSUBSCRIBE extends MethodType
  case CATCHUP extends MethodType
  case HEARTBEAT extends MethodType
  case GET_MESSAGES_BY_ID extends MethodType
  case GREET_SERVER extends MethodType

object MethodType:
  def apply(method: String): MethodType =
    method.trim.toLowerCase match
      case "broadcast"          => BROADCAST
      case "publish"            => PUBLISH
      case "subscribe"          => SUBSCRIBE
      case "unsubscribe"        => UNSUBSCRIBE
      case "catchup"            => CATCHUP
      case "heartbeat"          => HEARTBEAT
      case "get_messages_by_id" => GET_MESSAGES_BY_ID
      case "greet_server"       => GREET_SERVER
      case _                    => INVALID
