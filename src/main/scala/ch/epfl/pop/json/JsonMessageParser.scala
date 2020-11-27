package ch.epfl.pop.json

import ch.epfl.pop.json.JsonCommunicationProtocol._
import ch.epfl.pop.json.JsonMessages._
import ch.epfl.pop.json.JsonUtils.ErrorCodes.ErrorCodes
import ch.epfl.pop.json.JsonUtils.{ErrorCodes, JsonMessageParserError, JsonMessageParserException}
import spray.json._

import scala.util.{Failure, Success, Try}


/**
 * Custom Json Parser for our Json-RPC protocol
 */
object JsonMessageParser {

  /**
   * Parse a Json string into a JsonMessage
   *
   * @param source the input string
   * @return the parsed version of source
   */
  def parseMessage(source: String): Either[JsonMessage, JsonMessageParserError] = {

    def buildJsonMessageParserException(
                                         obj: JsObject,
                                         id: Int = JsonUtils.ID_NOT_FOUND,
                                         errorCode: ErrorCodes = ErrorCodes.InvalidData,
                                         description: String = ""): JsonMessageParserError = {

      val msg: String = if (description == "") errorCode.toString else description

      obj.getFields("id") match {
        case Seq(JsNumber(id)) => JsonMessageParserError(msg, id.toInt, errorCode)
        case _ => JsonMessageParserError(msg, JsonUtils.ID_NOT_FOUND, errorCode)
      }
    }


    Try(source.parseJson.asJsObject) match {
      case Success(obj) =>

        try {
          obj.getFields("jsonrpc") match {
            case Seq(JsString(v)) =>
              if (v != JsonUtils.JSON_RPC_VERSION)
                throw JsonMessageParserException("invalid \"jsonrpc\" field : invalid jsonrpc version")
            case _ => throw DeserializationException("invalid message : jsonrpc field missing or wrongly formatted")
          }

          obj.getFields("method") match {
            case Seq(JsString(m)) => m match {
              /* Subscribing to a channel */
              case Methods.Subscribe() => Left(obj.convertTo[SubscribeMessageClient])

              /* Unsubscribe from a channel */
              case Methods.Unsubscribe() => Left(obj.convertTo[UnsubscribeMessageClient])

              /* Propagate message on a channel */
              case Methods.Message() => Left(obj.convertTo[PropagateMessageServer])

              /* Catch up on past message on a channel */
              case Methods.Catchup() => Left(obj.convertTo[CatchupMessageClient])

              /* Publish on a channel + All Higher-level communication */
              case Methods.Publish() => Left(obj.convertTo[JsonMessagePublishClient])

              /* parsing error : invalid method value */
              case _ => throw JsonMessageParserException("invalid message : method value unrecognized")
            }
            case _ => throw JsonMessageParserException("invalid message : method field missing or wrongly formatted")
          }

        } catch {
          case JsonMessageParserException(msg, id, code) => Right(buildJsonMessageParserException(obj, id, code, msg))
          case DeserializationException(msg, _, _) => Right(buildJsonMessageParserException(obj, description = msg))
          case _ => Right(buildJsonMessageParserException(obj))
        }

      /* parsing error : String is not a valid JsObject */
      case Failure(_) => Right(JsonMessageParserError("Invalid Json formatting (not a valid JsonObject)"))
    }
  }

  /**
   * Serialize a JsonMessage into a string
   *
   * @param message the JsonMessage to serialize
   * @return the serialized version of JsonMessage
   */
  @throws(classOf[SerializationException])
  def serializeMessage(message: JsonMessage): String = message match {
    case _: JsonMessageAnswerServer => message match {
      case m: AnswerResultIntMessageServer => m.toJson.toString
      case m: AnswerResultArrayMessageServer => m.toJson.toString
      case m: AnswerErrorMessageServer => m.toJson.toString
      case m: PropagateMessageServer => m.toJson.toString
    }

    case _: JsonMessagePublishClient => message match {
      case m: CreateLaoMessageClient => m.toJson(JsonMessagePublishClientFormat.write).toString
      case m: UpdateLaoMessageClient => m.toJson(JsonMessagePublishClientFormat.write).toString
      case m: BroadcastLaoMessageClient => m.toJson(JsonMessagePublishClientFormat.write).toString
      case m: WitnessMessageMessageClient => m.toJson(JsonMessagePublishClientFormat.write).toString
      case m: CreateMeetingMessageClient => m.toJson(JsonMessagePublishClientFormat.write).toString
      case m: BroadcastMeetingMessageClient => m.toJson(JsonMessagePublishClientFormat.write).toString
    }

    case _: JsonMessagePubSubClient => message match {
      case m: SubscribeMessageClient => m.toJson.toString
      case m: UnsubscribeMessageClient => m.toJson.toString
      case m: CatchupMessageClient => m.toJson.toString
    }

    case _ => throw new SerializationException("Json serializer failed : invalid input message")
  }

  def serializeMessage(mc: MessageContent): String = mc.toJson.toString
}
