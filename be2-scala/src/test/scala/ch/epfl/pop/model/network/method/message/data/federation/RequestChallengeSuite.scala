package ch.epfl.pop.model.network.method.message.data.federation

import util.examples.Federation.FederationRequestChallengeExample._
import ch.epfl.pop.json.MessageDataProtocol.*
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import org.scalatest.matchers.should.Matchers
import org.scalatest.funsuite.AnyFunSuite as FunSuite
import spray.json.*

class RequestChallengeSuite extends FunSuite with Matchers {

  test("Constructor/apply works for RequestChallenge as expected") {
    REQUEST_CHALLENGE.timestamp should equal(TIMESTAMP)
    REQUEST_CHALLENGE._object should equal(ObjectType.federation)
    REQUEST_CHALLENGE.action should equal(ActionType.challenge_request)
  }

  test("BuildFromJson for RequestChallenge works as expected") {
    val request_challenge_ = FederationRequestChallenge.buildFromJson(REQUEST_CHALLENGE.toJson.toString)

    request_challenge_ should equal(REQUEST_CHALLENGE)
  }
}
