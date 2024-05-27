package ch.epfl.pop.model.network.method.message.data

enum ObjectType(val objectType: String):
  case INVALID extends ObjectType("INVALID")
  case lao extends ObjectType("lao")
  case message extends ObjectType("message")
  case meeting extends ObjectType("meeting")
  case roll_call extends ObjectType("roll_call")
  case election extends ObjectType("election")
  case chirp extends ObjectType("chirp")
  case reaction extends ObjectType("reaction")
  case coin extends ObjectType("coin")
  case popcha extends ObjectType("popcha")
  case federation extends ObjectType("federation")
