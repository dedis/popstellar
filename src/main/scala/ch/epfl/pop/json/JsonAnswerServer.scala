package ch.epfl.pop.json

import ch.epfl.pop.json.JsonAnswerServer.ErrorCodes.ErrorCodes
import ch.epfl.pop.json.JsonMessages.{AnswerErrorMessageServer, AnswerResultArrayMessageServer, AnswerResultIntMessageServer}



object JsonAnswerServer {

  object ErrorCodes extends Enumeration {
    type ErrorCodes = Value

    // operation was successful (should never be used)
    val Success: JsonAnswerServer.ErrorCodes.Value = Value(0, "operation was successful")
    // invalid action
    val InvalidAction: JsonAnswerServer.ErrorCodes.Value = Value(-1, "invalid action")
    // invalid resource (e.g. channel does not exist, channel was not subscribed to, etc.)
    val InvalidResource: JsonAnswerServer.ErrorCodes.Value = Value(-2, "invalid resource")
    // resource already exists (e.g. lao already exists, channel already exists, etc.)
    val AlreadyExists: JsonAnswerServer.ErrorCodes.Value = Value(-3, "resource already exists")
    // request data is invalid (e.g. message is invalid)
    val InvalidData: JsonAnswerServer.ErrorCodes.Value = Value(-4, "request data is invalid")
    // access denied (e.g. subscribing to a “restricted” channel)
    val AccessDenied: JsonAnswerServer.ErrorCodes.Value = Value(-5, "access denied")
  }

  def sendAnswer(answer: String): Unit = {
    println("======== (TODO) MESSAGE TO SEND TO CLIENT ========")
    println(answer)
    println("==================================================")
  }



  def sendResultIntAnswerTo(id: Int): Unit = sendAnswer(
    JsonMessageParser.serializeMessage(AnswerResultIntMessageServer(result = ErrorCodes.Success.id, id = id))
  )

  def sendResultArrayAnswerTo(id: Int, result: List[ChannelMessage]): Unit = sendAnswer(
    JsonMessageParser.serializeMessage(AnswerResultArrayMessageServer(result = ChannelMessages(result), id = id))
  )

  def sendErrorAnswerTo(id: Int, errorCode: ErrorCodes, description: String = ""): Unit = {
    val answer: AnswerErrorMessageServer = description match {
      // use default ErrorCode description
      case "" => AnswerErrorMessageServer(error = MessageErrorContent(errorCode.id, errorCode.toString), id = id)
      // use provided description
      case _ => AnswerErrorMessageServer(error = MessageErrorContent(errorCode.id, description), id = id)
    }

    sendAnswer(JsonMessageParser.serializeMessage(answer))
  }
}
