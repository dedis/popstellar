package ch.epfl.pop.model.network.method.message.data

enum ObjectType:
  case INVALID extends ObjectType
  case lao extends ObjectType
  case message extends ObjectType
  case meeting extends ObjectType
  case roll_call extends ObjectType
  case election extends ObjectType
  case chirp extends ObjectType
  case reaction extends ObjectType
  case coin extends ObjectType
  case popcha extends ObjectType

object ObjectType:
  override def toString: String =
    this.toString.toLowerCase

  def apply(objectType: String): ObjectType =
    objectType match
      case "lao"       => lao
      case "message"   => message
      case "meeting"   => meeting
      case "roll_call" => roll_call
      case "election"  => election
      case "chirp"     => chirp
      case "reaction"  => reaction
      case "coin"      => coin
      case "popcha"    => popcha
      case _           => INVALID
