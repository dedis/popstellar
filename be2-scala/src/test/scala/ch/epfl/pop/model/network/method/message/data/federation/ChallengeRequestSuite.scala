package ch.epfl.pop.model.network.method.message.data.federation

import util.examples.Federation.FederationChallengeRequestExample.*
import ch.epfl.pop.json.MessageDataProtocol.*
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import org.scalatest.matchers.should.Matchers
import org.scalatest.funsuite.AnyFunSuite as FunSuite
import spray.json.*
import util.examples.Federation.FederationChallengeRequestExample.CHALLENGE_REQUEST

class ChallengeRequestSuite extends FunSuite with Matchers {

  test("Constructor/apply works for RequestChallenge as expected") {
    CHALLENGE_REQUEST.timestamp should equal(TIMESTAMP)
    CHALLENGE_REQUEST._object should equal(ObjectType.federation)
    CHALLENGE_REQUEST.action should equal(ActionType.challenge_request)
  }

  test("BuildFromJson for RequestChallenge works as expected") {
    val request_challenge_ = FederationChallengeRequest.buildFromJson(CHALLENGE_REQUEST.toJson.toString)

    request_challenge_ should equal(CHALLENGE_REQUEST)
  }
}
