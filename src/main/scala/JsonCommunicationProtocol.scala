import spray.json.{DefaultJsonProtocol}

object JsonCommunicationProtocol extends DefaultJsonProtocol {

  implicit val attendeeFormat = jsonFormat2(Attendee)

}



// Test object for the protocol
//type Key = String // hex value in string formal

abstract class Participant
//case class Organizer(name: String, id: Key)
//case class Witness(name: String, id: Key)
case class Attendee(name: String, id: String)



