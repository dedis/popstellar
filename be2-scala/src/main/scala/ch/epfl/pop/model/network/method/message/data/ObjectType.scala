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
  def apply(objectType: String): ObjectType =
    objectType.trim.toLowerCase match
      case "lao"       => LAO
      case "message"   => MESSAGE
      case "meeting"   => MEETING
      case "roll_call" => ROLL_CALL
      case "election"  => ELECTION
      case "chirp"     => CHIRP
      case "reaction"  => REACTION
      case "coin"      => COIN
      case "popcha"    => POPCHA
      case _           => INVALID
