package ch.epfl.pop.model.network.method.message.data.federation

import ch.epfl.pop.json.MessageDataProtocol.*
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import ch.epfl.pop.model.objects.Base16Data
import org.scalatest.funsuite.AnyFunSuite as FunSuite
import org.scalatest.matchers.should.Matchers
import spray.json.*
import util.examples.Federation.FederationChallengeExample.*

class ChallengeSuite extends FunSuite with Matchers {

  test("Constructor/apply works for Challenge as expected") {
    CHALLENGE.value should equal(VALUE)
    CHALLENGE.validUntil should equal(VALID_UNTIL)
    CHALLENGE._object should equal(ObjectType.federation)
    CHALLENGE.action should equal(ActionType.challenge)

  }

  test("BuildFromJson for Challenge works as expected") {
    val challenge_ = FederationChallenge.buildFromJson(CHALLENGE.toJson.toString)

    challenge_ should equal(CHALLENGE)
    noException should be thrownBy VALUE
  }

  test("The system should only accept a Challenge value that corresponds to 32 bytes i.e a hexadecimal string of 64 characters") {
    an[IllegalArgumentException] should be thrownBy { FederationChallenge(Base16Data("56739ad12cf"), VALID_UNTIL) }
  }
}
