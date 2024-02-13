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
  def unapply(method: String): Option[MethodType] =
    method match
      case "__INVALID_METHOD__" => Some(INVALID)
      case "broadcast"          => Some(BROADCAST)
      case "publish"            => Some(PUBLISH)
      case "subscribe"          => Some(SUBSCRIBE)
      case "unsubscribe"        => Some(UNSUBSCRIBE)
      case "catchup"            => Some(CATCHUP)
      case "heartbeat"          => Some(HEARTBEAT)
      case "get_messages_by_id" => Some(GET_MESSAGES_BY_ID)
      case "greet_server"       => Some(GREET_SERVER)
      case _                    => None
