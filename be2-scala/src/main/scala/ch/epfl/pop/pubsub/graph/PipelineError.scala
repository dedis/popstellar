package ch.epfl.pop.pubsub.graph

/** All errors encountered during the graph traversal is embedded inside a [[PipelineError]]
  *
  * @param code
  *   error code of the error
  * @param description
  *   description of the error
  * @param rpcId
  *   rpc-id of the client request (if available)
  *
  * Note: the rpcId is used later in the graph at the 'Answerer' stage
  */
final case class PipelineError(code: Int, description: String, rpcId: Option[Int])

enum ErrorCodes(val id: Int, val err: String):
  // invalid action
  case INVALID_ACTION extends ErrorCodes(-1, "invalid action")
  // invalid resource (e.g. channel does not exist, channel was not subscribed to, etc.)
  case INVALID_RESOURCE extends ErrorCodes(-2, "invalid resource")
  // resource already exists (e.g. lao already exists, channel already exists, etc.)
  case ALREADY_EXISTS extends ErrorCodes(-3, "resource already exists")
  // request data is invalid (e.g. message is invalid)
  case INVALID_DATA extends ErrorCodes(-4, "request data is invalid")
  // access denied (e.g. subscribing to a “restricted” channel)
  case ACCESS_DENIED extends ErrorCodes(-5, "access denied")
  // internal server error (e.g. crashed while processing, database unavailable, etc.)
  case SERVER_ERROR extends ErrorCodes(-6, "internal server error")
