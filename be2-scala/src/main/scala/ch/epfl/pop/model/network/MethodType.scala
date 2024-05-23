package ch.epfl.pop.model.network

enum MethodType(val method: String):
  case INVALID extends MethodType("INVALID")
  case broadcast extends MethodType("broadcast")
  case publish extends MethodType("publish")
  case subscribe extends MethodType("subscribe")
  case unsubscribe extends MethodType("unsubscribe")
  case catchup extends MethodType("catchup")
  case heartbeat extends MethodType("heartbeat")
  case get_messages_by_id extends MethodType("get_messages_by_id")
  case greet_server extends MethodType("greet_server")
  case rumor extends MethodType("rumor")
  case paged_catchup extends MethodType("paged_catchup")
