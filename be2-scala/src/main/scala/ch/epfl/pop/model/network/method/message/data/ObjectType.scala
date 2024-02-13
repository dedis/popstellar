package ch.epfl.pop.model.network.method.message.data

enum ObjectType:
  case INVALID extends ObjectType
  case LAO extends ObjectType
  case MESSAGE extends ObjectType
  case MEETING extends ObjectType
  case ROLL_CALL extends ObjectType
  case ELECTION extends ObjectType
  case CHIRP extends ObjectType
  case REACTION extends ObjectType
  case COIN extends ObjectType
  case POPCHA extends ObjectType

object ObjectType:
  def unapply(objectType: String): Option[ObjectType] =
    objectType match
      case "__INVALID_OBJECT__" => Some(INVALID)
      case "lao"                => Some(LAO)
      case "message"            => Some(MESSAGE)
      case "meeting"            => Some(MEETING)
      case "roll_call"          => Some(ROLL_CALL)
      case "election"           => Some(ELECTION)
      case "chirp"              => Some(CHIRP)
      case "reaction"           => Some(REACTION)
      case "coin"               => Some(COIN)
      case "popcha"             => Some(POPCHA)
      case _                    => None
