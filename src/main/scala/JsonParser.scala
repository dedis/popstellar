import spray.json._
import JsonCommunicationProtocol._


object JsonParser {


  def main(args: Array[String]): Unit = {
    println("Json parser tests!")

    val source = """{ "some": "JSON source" }"""
    val jsonAst = source.parseJson // or JsonParser(source)

    val json: String = jsonAst.prettyPrint // or .compactPrint
    val json2: String = jsonAst.compactPrint

    val toJsonAttendee = Attendee("Nicolas", "123").toJson
    println(toJsonAttendee.prettyPrint)
    val fromJsonAttendee = toJsonAttendee.convertTo[Attendee]
    println(fromJsonAttendee.name)
    
  }
}
