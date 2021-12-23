package ch.epfl.pop.pubsub.graph.validator.lao

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL}
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.requests.lao.JsonRpcRequestCreateLao
import ch.epfl.pop.pubsub.graph.{GraphMessage,MessageDecoder,Validator}
import org.scalatest.{FlatSpec,GivenWhenThen,Inside,Matchers,Outcome}

import util.examples.CreateLaoExamples

class CreateLaoContentSuite extends FlatSpec with Matchers with Inside with GivenWhenThen {

  /**Decodes data before passing it to the test**/
  def withCreateLaoFixture(createLaoData: Message)(testCode: GraphMessage => Any) {
    // Raw encoded data data
    val message = Left(CreateLaoExamples.getJsonRequestFromMessage(createLaoData))
    println(message.getClass)
    // Decode data
    val decoded = MessageDecoder.parseData(message)
    println(decoded.getClass)
    decoded match {
      case Left(m: JsonRpcRequestCreateLao) =>
        println(m.getClass)
        testCode(decoded)
      case Left(m) =>
        println(m.getClass)
        fail(f"Decoder decoded to bad type: <$m> expected type is JsonRpcRequestCreateLao")
      case Right(_) =>
        fail("Message could not be decoded/parsed")
    }
  }

  behavior.of("A validator when receiving ")
  "a CreateLao data with valid content" should "be accepted by validator" in withCreateLaoFixture(CreateLaoExamples.createLao){
    Given("a valid decoded createLao request")
    (message) => {
      When("validated")
      val validationResult = Validator.validateMessageDataContent(message)
      inside(validationResult){
        case Left(msg) =>
          Then("the validation succeeds")
          msg shouldBe a [JsonRpcRequest]
        case _ @ Right(_) => fail("fails to validate CreateLao data content")
        case _ => fail(s"validated message <$validationResult> is of unexpected type")
      }
      And("the message has the same content after validation")
      validationResult should equal(message)
    }
  }
  //TODO: add tests for bad create lao data content
}
