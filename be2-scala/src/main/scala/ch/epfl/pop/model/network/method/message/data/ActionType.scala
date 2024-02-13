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
  def unapply(actionType: String): Option[ActionType] =
    actionType match
      case "__INVALID_ACTION__" => Some(INVALID)
      case "create"             => Some(CREATE)
      case "update_properties"  => Some(UPDATE_PROPERTIES)
      case "state"              => Some(STATE)
      case "greet"              => Some(GREET)
      case "witness"            => Some(WITNESS)
      case "open"               => Some(OPEN)
      case "reopen"             => Some(REOPEN)
      case "close"              => Some(CLOSE)
      // election actions:
      case "setup"     => Some(SETUP)
      case "result"    => Some(RESULT)
      case "end"       => Some(END)
      case "cast_vote" => Some(CAST_VOTE)
      case "key"       => Some(KEY)
      // social media actions:
      case "add"           => Some(ADD)
      case "delete"        => Some(DELETE)
      case "notify_add"    => Some(NOTIFY_ADD)
      case "notify_delete" => Some(NOTIFY_DELETE)
      // digital cash actions:
      case "post_transaction" => Some(POST_TRANSACTION)
      // popcha
      case "authenticate" => Some(AUTHENTICATE)
      case _              => None
