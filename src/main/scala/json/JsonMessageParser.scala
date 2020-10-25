package json

import spray.json._
import json.JsonMessages._
import json.JsonCommunicationProtocol._


/**
 * Custom Json Parser for our Json-RPC protocol
 */
final class JsonMessageParser {

  /**
   * Parse a Json string into a JsonMessage
   *
   * @param source the input string
   * @return the parsed version of source
   */
  @throws(classOf[DeserializationException])
  def parseMessage(source: String): JsonMessage = {

    val obj: JsObject = source.parseJson.asJsObject
    val fields: Map[String, JsValue] = obj.fields

    fields.head match {
      case ("create", values @ JsObject(_)) => values.convertTo[CreateChannelClient]
      case ("publish", values @ JsObject(_)) => values.convertTo[PublishChannelClient]
      case ("subscribe", values @ JsObject(_)) => values.convertTo[SubscribeChannelClient]
      case ("fetch", values @ JsObject(_)) => values.convertTo[FetchChannelClient]

      // TODO add more cases (need all groups to be on the same page before)

      case _ => throw DeserializationException(s"Invalid Json format or object header : ${fields.keys.head}")
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
    case m @ AnswerMessageServer(_, _) => m.toJson.toString
    case m @ NotifyChannelServer(_, _) => m.toJson.toString
    case m @ FetchChannelServer(_, _, _) => JsObject("event" -> m.toJson).toJson.toString
    case _ => throw new SerializationException("Json serializer failed : invalid input message")
  }
}
