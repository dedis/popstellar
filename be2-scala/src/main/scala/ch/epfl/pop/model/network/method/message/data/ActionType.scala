package ch.epfl.pop.model.network.method.message.data

enum ActionType:
  case INVALID extends ActionType
  case CREATE extends ActionType
  case UPDATE_PROPERTIES extends ActionType
  case STATE extends ActionType
  case GREET extends ActionType
  case WITNESS extends ActionType
  case OPEN extends ActionType
  case REOPEN extends ActionType
  case CLOSE extends ActionType
  // election actions:
  case SETUP extends ActionType
  case RESULT extends ActionType
  case END extends ActionType
  case CAST_VOTE extends ActionType
  case KEY extends ActionType
  // social media actions:
  case ADD extends ActionType
  case DELETE extends ActionType
  case NOTIFY_ADD extends ActionType
  case NOTIFY_DELETE extends ActionType
  // digital cash actions:
  case POST_TRANSACTION extends ActionType
  // popcha
  case AUTHENTICATE extends ActionType

object ActionType:
  def apply(actionType: String): ActionType =
    actionType.trim.toLowerCase match
      case "create"            => CREATE
      case "update_properties" => UPDATE_PROPERTIES
      case "state"             => STATE
      case "greet"             => GREET
      case "witness"           => WITNESS
      case "open"              => OPEN
      case "reopen"            => REOPEN
      case "close"             => CLOSE
      // election actions:
      case "setup"     => SETUP
      case "result"    => RESULT
      case "end"       => END
      case "cast_vote" => CAST_VOTE
      case "key"       => KEY
      // social media actions:
      case "add"           => ADD
      case "delete"        => DELETE
      case "notify_add"    => NOTIFY_ADD
      case "notify_delete" => NOTIFY_DELETE
      // digital cash actions:
      case "post_transaction" => POST_TRANSACTION
      // popcha
      case "authenticate" => AUTHENTICATE
      case _              => INVALID
