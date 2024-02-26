package ch.epfl.pop.model.network.method.message.data

enum ActionType:
  case INVALID extends ActionType
  case create extends ActionType
  case update_properties extends ActionType
  case state extends ActionType
  case greet extends ActionType
  case witness extends ActionType
  case open extends ActionType
  case reopen extends ActionType
  case close extends ActionType
  // election actions:
  case setup extends ActionType
  case result extends ActionType
  case end extends ActionType
  case cast_vote extends ActionType
  case key extends ActionType
  // social media actions:
  case add extends ActionType
  case delete extends ActionType
  case notify_add extends ActionType
  case notify_delete extends ActionType
  // digital cash actions:
  case post_transaction extends ActionType
  // popcha
  case authenticate extends ActionType

object ActionType:
  def apply(actionType: String): ActionType =
    actionType match
      case "create"            => create
      case "update_properties" => update_properties
      case "state"             => state
      case "greet"             => greet
      case "witness"           => witness
      case "open"              => open
      case "reopen"            => reopen
      case "close"             => close
      // election actions:
      case "setup"     => setup
      case "result"    => result
      case "end"       => end
      case "cast_vote" => cast_vote
      case "key"       => key
      // social media actions:
      case "add"           => add
      case "delete"        => delete
      case "notify_add"    => notify_add
      case "notify_delete" => notify_delete
      // digital cash actions:
      case "post_transaction" => post_transaction
      // popcha
      case "authenticate" => authenticate
      case _              => INVALID
