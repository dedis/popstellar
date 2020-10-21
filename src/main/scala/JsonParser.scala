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
    //println(s"Organizer's key : ${toJsonLao.convertTo[LaoCreationMessage].organizer}")


    // Simulate answers to frontEnd
    val positiveAnswerJson = JsonMessageAnswer(success = true, None).toJson
    val negativeAnswerJson = JsonMessageAnswer(success = false, Some("Error :> invalid format!")).toJson
    //println(positiveAnswerJson.prettyPrint)
    //println(negativeAnswerJson.prettyPrint)


    /* Class Json parsing */
    val testClassInst: TestClassJsonParsing = new TestClassJsonParsing(
      "test", 101, true, List("k1", "k3"), List(Attendee("Attendee1", "key1"), Attendee("Attendee2", "key2")), Some(3.5)
    )
    val testClassInstJson: JsValue = testClassInst.toJson
    //println(testClassInstJson.prettyPrint)
    //println(testClassInstJson.asJsObject().fields)

    val testClassInstCon: TestClassJsonParsing = testClassInstJson.convertTo[TestClassJsonParsing]
    //println(s"Parsing correct for testClass ? ${testClassInstJson.compactPrint == testClassInstCon.toJson.compactPrint}")

  }
}
