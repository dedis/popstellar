package json

import spray.json._
import json.JsonMessages._
import json.JsonCommunicationProtocol._


final class JsonMessageParser {

  def parseMessage(source: String): JsonMessage = {

    val obj: JsObject = source.parseJson.asJsObject
    val fields: Map[String, JsValue] = obj.fields

    fields.head match {
      case ("create", values @ JsObject(_)) => values.convertTo[SubscribeChannelClient]
      case ("publish", values @ JsObject(_)) => values.convertTo[PublishChannelClient]
      case ("subscribe", values @ JsObject(_)) => values.convertTo[SubscribeChannelClient]
      case ("fetch", values @ JsObject(_)) => values.convertTo[FetchChannelClient]

      // TODO add more cases (need all groups to be on the same page before)

      case _ => throw DeserializationException(s"Invalid Json format or object header : ${fields.keys.head}")
    }
  }

  def encodeMessage(message: JsonMessage): String = message match {
    case m @ AnswerMessageServer(_, _) => m.toJson.toString
    case m @ NotifyChannelServer(_, _) => m.toJson.toString
    case m @ FetchChannelServer(_, _, _) => m.toJson.toString
    case _ => throw DeserializationException("JsonEncoder failed : invalid input message") // TODO does the "inverse" exception exist?
  }

}

/*
DRAFT :

takes a Json string as input. Checks that it is a correct JSON format (but not the content of the message (input checking)
because we let the InputChecker (not implemented yet) do that)

JsonParser outputs a JsonMessage

 */