package ch.epfl.pop.model.network.method.message.data.federation

import ch.epfl.pop.json.MessageDataProtocol.*
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import org.scalatest.funsuite.AnyFunSuite as FunSuite
import org.scalatest.matchers.should.Matchers
import spray.json.*
import util.examples.Federation.FederationResultExample.*

class ResultSuite extends FunSuite with Matchers {

  test("Constructor/apply works for Result as expected") {
    RESULT_1.status should equal(STATUS_1)
    RESULT_1.organizer should equal(Some(ORGANIZER))
    RESULT_1.challenge_message should equal(CHALLENGE_MESSAGE)
    RESULT_1._object should equal(ObjectType.federation)
    RESULT_1.action should equal(ActionType.federation_result)

    RESULT_2.status should equal(STATUS_2)
    RESULT_2.reason should equal(Some(REASON))
    RESULT_2.challenge_message should equal(CHALLENGE_MESSAGE)
    RESULT_2._object should equal(ObjectType.federation)
    RESULT_2.action should equal(ActionType.federation_result)

    RESULT_1.challenge_message.value should equal(CHALLENGE_MESSAGE.value)
    RESULT_1.challenge_message.validUntil should equal(CHALLENGE_MESSAGE.validUntil)

    RESULT_2.challenge_message.value should equal(CHALLENGE_MESSAGE.value)
    RESULT_2.challenge_message.validUntil should equal(CHALLENGE_MESSAGE.validUntil)

  }
  test("BuildFromJson for Result works as expected") {
    val result_1_ = FederationResult.buildFromJson(RESULT_1.toJson.toString)
    val result_2_ = FederationResult.buildFromJson(RESULT_2.toJson.toString)
    result_1_ should equal(RESULT_1)
    result_2_ should equal(RESULT_2)
  }
}
