package ch.epfl.pop.pubsub.graph.validator.lao

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import ch.epfl.pop.pubsub.MessageRegistry
import ch.epfl.pop.pubsub.graph.{GraphMessage, MessageDecoder, Validator}
import org.scalatest.{GivenWhenThen, Inside}
import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatest.matchers.should.Matchers
import util.examples.lao.CreateLaoExamples

class CreateLaoContentSuite extends FlatSpec with Matchers with Inside with GivenWhenThen {

  /** Decodes data before passing it to the test * */
  def withCreateLaoFixture(createLaoData: Message)(testCode: GraphMessage => Any): Unit = {
    // Raw encoded data data
    val message = Right(CreateLaoExamples.getJsonRequestFromMessage(createLaoData))
    // Decode data
    val decoded = MessageDecoder.parseData(message, MessageRegistry.apply())
    decoded match {
      case Right(r: JsonRpcRequest) =>
        r.getDecodedDataHeader should equal((ObjectType.LAO, ActionType.CREATE))
        testCode(decoded)
      case Right(m) => fail(f"Decoder decoded to bad type: <$m> expected type is JsonRpcRequestCreateLao")
      case Left(_) =>
        fail("Message could not be decoded/parsed")
    }
  }

  behavior.of("A validator when receiving ")
  "a CreateLao data with valid content" should "be accepted by validator" in withCreateLaoFixture(CreateLaoExamples.createLao) {
    Given("a valid decoded createLao request")
    (message) => {
      When("validated")
      inside(message) {
        case Right(rpcRequest: JsonRpcRequest) =>
          val registry: MessageRegistry = MessageRegistry.apply()
          val validationResult = Validator.validateMessageDataContent(rpcRequest, registry)
          inside(validationResult) {
            case Right(msg) =>
              Then("the validation succeeds")
              msg shouldBe a[JsonRpcRequest]
            case _ @Left(_) => fail("fails to validate CreateLao data content")
            case _          => fail(s"validated message <$validationResult> is of unexpected type")
          }
          And("the message has the same content after validation")
          validationResult should equal(message)
        case _ => fail("fails to contain a JsonRpcRequest")
      }
    }
  }
  // TODO: add tests for bad create lao data content
}
