package ch.epfl.pop.pubsub.graph

final case class PipelineError(code: Int, description: String)

object ErrorCodes extends Enumeration {
  type ErrorCodes = Value

  // operation was successful (should never be used)
  val Success: ErrorCodes = Value(0, "operation was successful")
  // invalid action
  val InvalidAction: ErrorCodes = Value(-1, "invalid action")
  // invalid resource (e.g. channel does not exist, channel was not subscribed to, etc.)
  val InvalidResource: ErrorCodes = Value(-2, "invalid resource")
  // resource already exists (e.g. lao already exists, channel already exists, etc.)
  val AlreadyExists: ErrorCodes = Value(-3, "resource already exists")
  // request data is invalid (e.g. message is invalid)
  val InvalidData: ErrorCodes = Value(-4, "request data is invalid")
  // access denied (e.g. subscribing to a “restricted” channel)
  val AccessDenied: ErrorCodes = Value(-5, "access denied")
}
