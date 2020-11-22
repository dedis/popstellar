package json

import org.scalatest.FunSuite

import ch.epfl.pop.json._ // NOT useless import!



class JsonAnswerServerTest extends FunSuite {

  test("JsonAnswerServer.sendResultIntAnswerTo") {
    assertCompiles("JsonAnswerServer.sendResultIntAnswerTo(13)")
  }

  test("JsonAnswerServer.sendResultArrayAnswerTo") {
    assertCompiles("JsonAnswerServer.sendResultArrayAnswerTo(22, List(\"M1\", \"M2\", \"M3\"))")
    assertCompiles("JsonAnswerServer.sendResultArrayAnswerTo(22, List())")
  }

  test("JsonAnswerServer.sendErrorAnswerTo") {
    import ch.epfl.pop.json.JsonAnswerServer.ErrorCodes // NOT useless import!

    assertCompiles("JsonAnswerServer.sendErrorAnswerTo(14, ErrorCodes.InvalidResource)")
    assertCompiles("JsonAnswerServer.sendErrorAnswerTo(15, ErrorCodes.AccessDenied, \"Err\")")
  }
}
