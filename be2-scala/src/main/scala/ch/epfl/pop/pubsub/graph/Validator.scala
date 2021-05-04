package ch.epfl.pop.pubsub.graph

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.requests.lao.{JsonRpcRequestCreateLao, JsonRpcRequestStateLao, JsonRpcRequestUpdateLao}
import ch.epfl.pop.model.network.requests.meeting.{JsonRpcRequestCreateMeeting, JsonRpcRequestStateMeeting}
import ch.epfl.pop.model.network.requests.rollCall.{JsonRpcRequestCloseRollCall, JsonRpcRequestCreateRollCall, JsonRpcRequestOpenRollCall, JsonRpcRequestReopenRollCall}
import ch.epfl.pop.model.network.requests.witness.JsonRpcRequestWitnessMessage
import ch.epfl.pop.pubsub.graph.validators.LaoValidator.{validateCreateLao, validateStateLao, validateUpdateLao}
import ch.epfl.pop.pubsub.graph.validators.MeetingValidator.{validateCreateMeeting, validateStateMeeting}
import ch.epfl.pop.pubsub.graph.validators.RollCallValidator.{validateCloseRollCall, validateCreateRollCall, validateOpenRollCall, validateReopenRollCall}
import ch.epfl.pop.pubsub.graph.validators.WitnessValidator.validateWitnessMessage

object Validator {

  // FIXME implement schema
  def validateSchema(jsonString: JsonString): Either[JsonString, PipelineError] = Left(jsonString)

  // FIXME implement rpc validator
  def validateJsonRpcContent(graphMessage: GraphMessage): GraphMessage = graphMessage match {
    case Left(jsonRpcMessage) => jsonRpcMessage match {
      case _ => ???
    }
    case graphMessage@_ => graphMessage
  }

  def validateMessageContent(graphMessage: GraphMessage): GraphMessage = graphMessage match {
    case Left(jsonRpcMessage) => jsonRpcMessage match {
      case message@(_: JsonRpcRequestCreateLao) => validateCreateLao(message)
      case message@(_: JsonRpcRequestStateLao) => validateStateLao(message)
      case message@(_: JsonRpcRequestUpdateLao) => validateUpdateLao(message)
      case message@(_: JsonRpcRequestCreateMeeting) => validateCreateMeeting(message)
      case message@(_: JsonRpcRequestStateMeeting) => validateStateMeeting(message)
      case message@(_: JsonRpcRequestCreateRollCall) => validateCreateRollCall(message)
      case message@(_: JsonRpcRequestOpenRollCall) => validateOpenRollCall(message)
      case message@(_: JsonRpcRequestReopenRollCall) => validateReopenRollCall(message)
      case message@(_: JsonRpcRequestCloseRollCall) => validateCloseRollCall(message)
      case message@(_: JsonRpcRequestWitnessMessage) => validateWitnessMessage(message)
      case _ => Right(PipelineError(
        ErrorCodes.SERVER_FAULT.id,
        "Internal server fault: Validator was given a message it could not recognize"
      ))
    }
    case graphMessage@_ => graphMessage
  }


  // takes a string (json) input and compares it with the JsonSchema
  // /!\ Json Schema plugin?
  val schemaValidator: Flow[JsonString, Either[JsonString, PipelineError], NotUsed] = Flow[JsonString].map(validateSchema)

  // takes a JsonRpcMessage and validates input until Message layer
  val jsonRpcContentValidator: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map(validateJsonRpcContent)

  // validation from Message layer
  val messageContentValidator: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map(validateMessageContent)
}
