package ch.epfl.pop.model.network.method.message.data.federation

import ch.epfl.pop.json.MessageDataProtocol.*
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import org.scalatest.funsuite.AnyFunSuite as FunSuite
import org.scalatest.matchers.should.Matchers
import spray.json.*
import util.examples.Federation.FederationExpectExample.*

class ExpectSuite extends FunSuite with Matchers {

  test("Constructor/apply works for Expect as expected") {
    EXPECT.laoId should equal(LAO_ID)
    EXPECT.serverAddress should equal(SERVER_ADDRESS)
    EXPECT.other_organizer should equal(OTHER_ORGANIZER)
    EXPECT.challenge should equal(CHALLENGE)
    EXPECT._object should equal(ObjectType.federation)
    EXPECT.action should equal(ActionType.expect)

    EXPECT.challenge.data should equal(DATA)
    EXPECT.challenge.sender should equal(SENDER)
    EXPECT.challenge.signature should equal(SIGNATURE)
    EXPECT.challenge.message_id should equal(MESSAGE_ID)
  }

  test("BuildFromJson for Expect works as expected") {
    val expect_ = FederationExpect.buildFromJson(EXPECT.toJson.toString)

    expect_ should equal(EXPECT)
    noException should be thrownBy SERVER_ADDRESS
  }

  test("The system only accepts valid server addresses") {
    val WRONG_ADDRESS: String = "wss:/ethz.ch:9000/server"
    an[IllegalArgumentException] should be thrownBy FederationExpect(LAO_ID, WRONG_ADDRESS, OTHER_ORGANIZER, CHALLENGE)
  }
}
