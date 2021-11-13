package ch.epfl.pop.pubsub.graph

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.method._
import ch.epfl.pop.model.network.requests.election.{JsonRpcRequestEndElection, JsonRpcRequestResultElection, JsonRpcRequestSetupElection, JsonRpcRequestCastVoteElection}
import ch.epfl.pop.model.network.requests.lao.{JsonRpcRequestCreateLao, JsonRpcRequestStateLao, JsonRpcRequestUpdateLao}
import ch.epfl.pop.model.network.requests.meeting.{JsonRpcRequestCreateMeeting, JsonRpcRequestStateMeeting}
import ch.epfl.pop.model.network.requests.rollCall.{JsonRpcRequestCloseRollCall, JsonRpcRequestCreateRollCall, JsonRpcRequestOpenRollCall, JsonRpcRequestReopenRollCall}
import ch.epfl.pop.model.network.requests.witness.JsonRpcRequestWitnessMessage
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.pubsub.graph.validators.ElectionValidator._
import ch.epfl.pop.pubsub.graph.validators.LaoValidator._
import ch.epfl.pop.pubsub.graph.validators.MeetingValidator._
import ch.epfl.pop.pubsub.graph.validators.MessageValidator._
import ch.epfl.pop.pubsub.graph.validators.ParamsValidator._
import ch.epfl.pop.pubsub.graph.validators.RollCallValidator._
import ch.epfl.pop.pubsub.graph.validators.RpcValidator._
import ch.epfl.pop.pubsub.graph.validators.WitnessValidator._

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.ValidationMessage
import com.networknt.schema.SpecVersion

import scala.io.{BufferedSource, Source}
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._

import spray.json._
import java.io.InputStream


object Validator {
  private final val queryPath = "protocol/query/query.json" //With respect to resource folder
  private def validationError(rpcId: Option[Int]): PipelineError = PipelineError(
    ErrorCodes.INVALID_ACTION.id,
    "Unsupported action: Validator was given a message it could not recognize",
    rpcId
  )

  private def extractRpcId(jsonString: JsonString): Option[Int] = {
    Try(jsonString.parseJson.asJsObject.getFields("id")) match {
      case Success(Seq(JsNumber(id))) => Some(id.toInt)
      case _ => None
    }
  }

  //Get input stream of query.json file from resources folder
  final def queryFile: InputStream = this.getClass().getClassLoader().getResourceAsStream(queryPath);

  def validateSchema(jsonString: JsonString): Either[JsonString, PipelineError] = {

    val objectMapper: ObjectMapper = new ObjectMapper()
    // Creation of a JsonSchemaFactory that supports the DraftV07 with the schema obtaines from a node created from query.json
    val factory: JsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
    // Creation of a JsonNode using the readTree function from the file query.json (at queryPath)
    // FIXME: error handling for queryPath
    val jsonSchemaNode: JsonNode = objectMapper.readTree(queryFile)
    queryFile.close()
    // Creation of a JsonSchema from the previously created factory and JsonNode
    val schema: JsonSchema = factory.getSchema(jsonSchemaNode)

    // Creation of a JsonNode containing the information from the input jsonString
    val jsonNode: JsonNode = objectMapper.readTree(jsonString)
    // Validation of the input, the result is a set of errors (if no errors, size == 0)
    val errors: Set[ValidationMessage] = schema.validate(jsonNode).asScala.toSet

    if (errors.isEmpty){
      Left(jsonString)
    }
    else {
      val rpcId = extractRpcId(jsonString)
      // we get and concatenate all of the JsonString messages
      Right(PipelineError(ErrorCodes.INVALID_DATA.id, errors.mkString("; "), rpcId))
    }

  }

  private def validateJsonRpcContent(graphMessage: GraphMessage): GraphMessage = graphMessage match {
    case Left(jsonRpcMessage) => jsonRpcMessage match {
      case message@(_: JsonRpcRequest) => validateRpcRequest(message)
      case message@(_: JsonRpcResponse) => validateRpcResponse(message)
      case _ => Right(validationError(None)) // should never happen
    }
    case graphMessage@_ => graphMessage
  }

  private def validateMethodContent(graphMessage: GraphMessage): GraphMessage = graphMessage match {
    case Left(jsonRpcRequest: JsonRpcRequest) => jsonRpcRequest.getParams match {
      case _: Broadcast => validateBroadcast(jsonRpcRequest)
      case _: Catchup => validateCatchup(jsonRpcRequest)
      case _: Publish => validatePublish(jsonRpcRequest)
      case _: Subscribe => validateSubscribe(jsonRpcRequest)
      case _: Unsubscribe => validateUnsubscribe(jsonRpcRequest)
      case _ => Right(validationError(jsonRpcRequest.id))
    }
    case Left(jsonRpcResponse: JsonRpcResponse) => Right(PipelineError(
      ErrorCodes.SERVER_ERROR.id,
      "Unsupported action: MethodValidator was given a response message",
      jsonRpcResponse.id
    ))
    case graphMessage@_ => graphMessage
  }

  private def validateMessageContent(graphMessage: GraphMessage): GraphMessage = graphMessage match {
    case Left(jsonRpcRequest: JsonRpcRequest) => jsonRpcRequest.getParams match {
      case _: Broadcast => validateMessage(jsonRpcRequest)
      case _: Catchup => graphMessage
      case _: Publish => validateMessage(jsonRpcRequest)
      case _: Subscribe => graphMessage
      case _: Unsubscribe => graphMessage
      case _ => Right(validationError(jsonRpcRequest.id))
    }
    case graphMessage@_ => graphMessage
  }

  def validateHighLevelMessage(graphMessage: GraphMessage): GraphMessage = graphMessage match {
    case Left(_) => validateJsonRpcContent(graphMessage) match {
      case Left(_) => validateMethodContent(graphMessage) match {
        case Left(_) => validateMessageContent(graphMessage) match {
          case Left(_) => graphMessage
          case graphMessage@_ => graphMessage
        }
        case graphMessage@_ => graphMessage
      }
      case graphMessage@_ => graphMessage
    }
    case graphMessage@_ => graphMessage
  }

  def validateMessageDataContent(graphMessage: GraphMessage): GraphMessage = graphMessage match {
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
      case message@(_: JsonRpcRequestSetupElection) => validateSetupElection(message)
      case message@(_: JsonRpcRequestCastVoteElection) => validateCastVoteElection(message)
      case message@(_: JsonRpcRequestResultElection) => validateResultElection(message)
      case message@(_: JsonRpcRequestEndElection) => validateEndElection(message)
      case message@(_: JsonRpcRequestWitnessMessage) => validateWitnessMessage(message)
      case _ => Right(validationError(jsonRpcMessage match {
        case r: JsonRpcRequest => r.id
        case r: JsonRpcResponse => r.id
        case _ => None
      }))
    }
    case graphMessage@_ => graphMessage
  }


  // takes a string (json) input and compares it with the JsonSchema
  // /!\ Json Schema plugin?
  val schemaValidator: Flow[JsonString, Either[JsonString, PipelineError], NotUsed] = Flow[JsonString].map(validateSchema)

  // takes a JsonRpcMessage and validates input until Message layer
  val jsonRpcContentValidator: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map(validateHighLevelMessage)

  // validation from Message layer
  val messageContentValidator: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map(validateMessageDataContent)
}
