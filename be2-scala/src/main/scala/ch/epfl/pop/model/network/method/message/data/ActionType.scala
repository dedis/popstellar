package ch.epfl.pop.model.network.method.message.data

enum ActionType(val action: String):
  case INVALID extends ActionType("INVALID")
  case create extends ActionType("create")
  case update_properties extends ActionType("update_properties")
  case state extends ActionType("state")
  case greet extends ActionType("greet")
  case witness extends ActionType("witness")
  case open extends ActionType("open")
  case reopen extends ActionType("reopen")
  case close extends ActionType("close")
  // election actions:
  case setup extends ActionType("setup")
  case result extends ActionType("result")
  case end extends ActionType("end")
  case cast_vote extends ActionType("cast_vote")
  case key extends ActionType("key")
  // social media actions:
  case add extends ActionType("add")
  case delete extends ActionType("delete")
  case notify_add extends ActionType("notify_add")
  case notify_delete extends ActionType("notify_delete")
  // digital cash actions:
  case post_transaction extends ActionType("post_transaction")
  // popcha
  case authenticate extends ActionType("authenticate")
  // federation actions
  case init extends ActionType("init")
  case expect extends ActionType("expect")
  case challenge_request extends ActionType("challenge_request")
  case challenge extends ActionType("challenge")
  case federation_result extends ActionType("federation_result")
