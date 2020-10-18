import spray.json._
import JsonCommunicationProtocol._


object JsonParser {


  def main(args: Array[String]): Unit = {

    /* Simple Json parsing example */
    //val source = """{ "some": "JSON source" }"""
    //val jsonAst = source.parseJson // or JsonParser(source)

    //val json: String = jsonAst.prettyPrint // or .compactPrint


    /* Simple Json Case class parsing */
    val toJsonLao: JsValue = LaoCreationMessage("Event0", "pkOrganizer", List("pkW1", "pkW2", "pkW3"), "attestation").toJson
    //println(toJsonLao.prettyPrint)
    val fromJsonEvent: LaoCreationMessage = toJsonLao.convertTo[LaoCreationMessage]
    //println(s"Organizer's key : ${fromJsonEvent.organizer}")

    // Simulate answers to frontEnd
    val positiveAnswerJson = JsonMessageAnswer(success = true, None).toJson
    //println(positiveAnswerJson.prettyPrint)
    val negativeAnswerJson = JsonMessageAnswer(success = false, Some("Error :> invalid format!")).toJson
    //println(negativeAnswerJson.prettyPrint)


    /* Class Json parsing */
    val testClassInst: TestClassJsonParsing = new TestClassJsonParsing(
      "test", 101, true, List("k1", "k2"), List(Attendee("Attendee1", "key1"), Attendee("Attendee2", "key2")), Some(3.5)
    )
    val testClassInstJson = testClassInst.toJson
    println(testClassInstJson.prettyPrint)

    val testClassInstCon = testClassInstJson.convertTo[TestClassJsonParsing]
    //println(s"TestClass attendees : ${testClassInstCon.attendees}")
  }
}
