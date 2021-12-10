package ch.epfl.pop.pubsub.graph.validator

import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.Outcome
import util.examples.CreateLaoExamples
import ch.epfl.pop.pubsub.graph.Validator
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.pubsub.graph.MessageDecoder
import ch.epfl.pop.pubsub.graph.GraphMessage
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.GraphDSL
import akka.NotUsed
import akka.stream.FlowShape
import ch.epfl.pop.model.network.method.message.Message
import org.scalatest.GivenWhenThen
import ch.epfl.pop.model.network.JsonRpcResponse
import org.scalatest.Inside

class MessageDataContentValidatorSuite extends FlatSpec with Matchers with Inside with GivenWhenThen {

  /**Decodes data before passing it to the test**/
  def withCreateLaoFixture(createLaoData: Message)(testCode: GraphMessage => Any) {
    // Raw encoded data data
    val message = Left(CreateLaoExamples.getJsonRequestFromMessage(createLaoData))
    // Decode data
    val decoded = MessageDecoder.parseData(message)
    decoded match {
      case decoded @ Left(_) =>
        testCode(decoded)
      case decoded @ Right(_) =>
        fail("Message could not be decoded/parsed")
    }
  }

  behavior.of("A validator when receiving ")
  "a CreateLao data with valid content" should "be accepted by validator" in withCreateLaoFixture(CreateLaoExamples.createLao){
    Given("a valid decoded createLao request ")
    (msg) => {
      When("validated")
      val validated = Validator.validateMessageDataContent(msg)
      inside(validated){
        case Left(msg) => {
          Then("the validation succeeds")
          msg shouldBe a [JsonRpcRequest]
        }
        case _ @ Right(_) => fail("fails to validate a valid CreateLao data content")
        case _ => fail(s"validated message <$validated> is of unexcpected type")
      }
      And("the message has the same content after validation")
      validated should equal(msg)
    }
  }
  //TODO: add tests for bad create lao data content
}
