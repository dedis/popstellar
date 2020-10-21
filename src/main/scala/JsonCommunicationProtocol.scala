import spray.json._
import types._

import scala.collection.immutable.TreeMap


object JsonCommunicationProtocol extends DefaultJsonProtocol {

  // Parsing for case classes
  implicit val attendeeFormat: RootJsonFormat[Attendee] = jsonFormat2(Attendee)
  implicit val laoCreationFormat: RootJsonFormat[LaoCreationMessage] = jsonFormat4(LaoCreationMessage)


  implicit val messageAnswerFormat: RootJsonFormat[JsonMessageAnswer] = jsonFormat2(JsonMessageAnswer)

  // Parsing for classes
  implicit object TestClassFormat extends RootJsonFormat[TestClassJsonParsing] {
    override def read(json: JsValue): TestClassJsonParsing = json.asJsObject.getFields("testClass") match {
      case Seq(values @ JsObject(_)) =>
        values.getFields("name", "number", "b", "keys", "attendees", "additional") match {
          case Seq(JsString(name), JsNumber(num), JsBoolean(b), JsArray(keys), JsArray(att), optional) =>
            new TestClassJsonParsing(
              name,
              num.toInt,
              b,
              keys.map(_.convertTo[Key]).toList,
              att.map(_.convertTo[Attendee]).toList,
              optional match {
                case JsNumber(d) => Some(d.toDouble)
                case _ => None
              }
            )
          case _ => throw DeserializationException("invalid testClassFormat values")
        }
      case _ => throw DeserializationException("invalid TestClassFormat header")
    }

    override def write(obj: TestClassJsonParsing): JsValue = {

      val additional: JsValue = obj.additional match {
        case Some(d) => JsNumber(d)
        case _ => JsNull
      }

      val content: JsObject = JsObject(
        "name" -> JsString(obj.name),
        "number" -> JsNumber(obj.number),
        "b" -> JsBoolean(obj.b),
        "keys" -> JsArray(obj.keys.map(e => e.toJson).toVector),
        "attendees" -> JsArray(obj.attendees.map(e => e.toJson).toVector),
        "additional" -> additional
      )

      JsObject("testClass" -> content)
    }

  }
}

object TreeExtractor {
  def unapply[K,V](m: TreeMap[K,V]): Option[((K,V), TreeMap[K,V])] =
    m.headOption.map((_, m.tail))
}

package object types {
  type Key = String // hex value in string formal
  type Timestamp = String // ???
  type Hash = String //  ???
  type Hex = String // hex in string format
}


sealed abstract class Participant
//case class Organizer(name: String, id: Key) extends Participant
//case class Witness(name: String, id: Key) extends Participant
case class Attendee(name: String, id: Key) extends Participant


sealed abstract class JsonMessage
case class LaoCreationMessage(
  name: String,
  organizer: Key,
  witnesses: List[Key],
  attestation: Hex
) extends JsonMessage


case class JsonMessageAnswer(success: Boolean, error: Option[String]) extends JsonMessage


sealed class TestClassJsonParsing(
 val name: String,
 val number: Int,
 val b: Boolean,
 val keys: List[Key],
 val attendees: List[Attendee],
 val additional: Option[Double]
)
