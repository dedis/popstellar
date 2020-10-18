import spray.json._
import types._


object JsonCommunicationProtocol extends DefaultJsonProtocol {

  // Parsing for case classes
  implicit val attendeeFormat: RootJsonFormat[Attendee] = jsonFormat2(Attendee)
  implicit val laoCreationFormat: RootJsonFormat[LaoCreationMessage] = jsonFormat4(LaoCreationMessage)


  implicit val messageAnswerFormat: RootJsonFormat[JsonMessageAnswer] = jsonFormat2(JsonMessageAnswer)

  // Parsing for classes
  implicit object TestClassFormat extends RootJsonFormat[TestClassJsonParsing] {
    override def read(json: JsValue): TestClassJsonParsing = json match {
      case JsArray(Vector(JsString(name), JsNumber(number), JsBoolean(b), JsArray(keys), JsArray(attendees), additional)) =>
        new TestClassJsonParsing(
          name,
          number.toInt,
          b,
          keys.map(_.convertTo[Key]).toList,
          attendees.map(_.convertTo[Attendee]).toList,
          additional match {
            case JsNumber(d) => Some(d.toDouble)
            case _ => None
          }
        )
      case _ => throw DeserializationException("invalid TestClassFormat input")
    }

    override def write(obj: TestClassJsonParsing): JsValue = {
      JsArray(
        JsString(obj.name),
        JsNumber(obj.number),
        JsBoolean(obj.b),
        JsArray(obj.keys.map(e => e.toJson).toVector),
        JsArray(obj.attendees.map(e => e.toJson).toVector),
        obj.additional match {
          case Some(d) => JsNumber(d)
          case _ => JsNull
        }
      )
    }

  }
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
