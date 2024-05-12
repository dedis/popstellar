package ch.epfl.pop.model.network.method.message.data.federation

import ch.epfl.pop.json.MessageDataProtocol.*
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import org.scalatest.funsuite.AnyFunSuite as FunSuite
import org.scalatest.matchers.should.Matchers
import spray.json.*
import util.examples.Federation.FederationInitExample.*

class InitSuite extends FunSuite with Matchers {

  test("Constructor/apply works for Init as expected") {

    INIT.laoId should equal(LAO_ID)
    INIT.serverAddress should equal(SERVER_ADDRESS)
    INIT.other_organizer should equal(OTHER_ORGANIZER)
    INIT.challenge should equal(CHALLENGE)
    INIT._object should equal(ObjectType.federation)
    INIT.action should equal(ActionType.init)

    INIT.challenge.data should equal(DATA)
    INIT.challenge.sender should equal(SENDER)
    INIT.challenge.signature should equal(SIGNATURE)
    INIT.challenge.message_id should equal(MESSAGE_ID)

  }
  test("BuildFromJson for Init works as expected") {

    val init_ = FederationInit.buildFromJson(INIT.toJson.toString)

    init_ should equal(INIT)
    noException should be thrownBy SERVER_ADDRESS
  }

  test("The system only accepts valid server addresses") {
    val WRONG_ADDRESS: String = "ss:/epfl.ch:9000/server"
    an[IllegalArgumentException] should be thrownBy FederationExpect(LAO_ID, WRONG_ADDRESS, OTHER_ORGANIZER, CHALLENGE)
  }

}
