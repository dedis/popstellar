package ch.epfl.pop.json

import ch.epfl.pop.json.JsonCommunicationProtocol._
import ch.epfl.pop.json.JsonMessages._
import spray.json._


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
  @throws(classOf[DeserializationException])
  def parseMessage(source: String): JsonMessage = {

    val obj: JsObject = source.parseJson.asJsObject

    obj.getFields("jsonrpc") match {
      case Seq(JsString(v)) => if (v != "2.0") throw DeserializationException("TODO send error code")
      case _ => throw DeserializationException("invalid message : jsonrpc field missing or wrongly formatted")
    }

    obj.getFields("method") match {
      case Seq(JsString(m)) => m match {
        /* Subscribing to a channel */
        case Methods.Subscribe() => obj.convertTo[SubscribeMessageClient]

        /* Unsubscribing from a channel */
        case Methods.Unsubscribe() => obj.convertTo[UnsubscribeMessageClient]

        /* Propagating message on a channel */
        case Methods.Message() => obj.convertTo[PropagateMessageServer]

        /* Catching up on past message on a channel */
        case Methods.Catchup() => obj.convertTo[CatchupMessageClient]

        /* Publish on a channel + All Higher-level communication */
        case Methods.Publish() => obj.convertTo[JsonMessageAdminClient]

        /* parsing error : invalid method value */
        case _ => throw DeserializationException("invalid message : method value unrecognized")
      }
      case _ => throw DeserializationException("invalid message : method field missing or wrongly formatted")
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

    case _: JsonMessageAdminClient => message match {
      case m: CreateLaoMessageClient => m.toJson(JsonMessageAdminClientFormat.write).toString
      case m: UpdateLaoMessageClient => m.toJson(JsonMessageAdminClientFormat.write).toString
      case m: BroadcastLaoMessageClient => m.toJson(JsonMessageAdminClientFormat.write).toString
      case m: WitnessMessageMessageClient => m.toJson(JsonMessageAdminClientFormat.write).toString
      case m: CreateMeetingMessageClient => m.toJson(JsonMessageAdminClientFormat.write).toString
      case m: BroadcastMeetingMessageClient => m.toJson(JsonMessageAdminClientFormat.write).toString
    }

    case _: JsonMessagePubSubClient => message match {
      case m: SubscribeMessageClient => m.toJson.toString
      case m: UnsubscribeMessageClient => m.toJson.toString
      case m: CatchupMessageClient => m.toJson.toString
    }

    case _ => throw new SerializationException("Json serializer failed : invalid input message")
  }
}
