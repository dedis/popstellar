package ch.epfl.pop.pubsub.graph.validator

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.pubsub.graph.GraphMessage
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.MethodType
import ch.epfl.pop.model.network.method.ParamsWithMessage
import ch.epfl.pop.model.objects.Channel
import util.examples.CreateLaoExamples
import ch.epfl.pop.pubsub.graph.MessageDecoder
import ch.epfl.pop.model.network.requests.lao.JsonRpcRequestCreateLao
import org.scalatest.GivenWhenThen
import ch.epfl.pop.model.network.requests.lao.JsonRpcRequestStateLao
import ch.epfl.pop.model.network.method.message.data.lao.CreateLao
import ch.epfl.pop.pubsub.graph.ErrorCodes
import ch.epfl.pop.pubsub.graph.PipelineError
import org.scalatest.Inside

class MessageDecoderSuite extends FlatSpec with Matchers with Inside with GivenWhenThen {

  def withCreateLaoFixiture(msg: Message)(testCode: (GraphMessage, Message) => Any){
    val jsonReq = CreateLaoExamples.getJsonRequestFromMessage(msg)
    testCode(Left(jsonReq),msg)
  }

  def testGoodFormat =
      (gm: GraphMessage, createLaoMessage: Message) => {
        Given("a correct graph message of JsonRpcRequest")
        //gm
        And("a createLao message")
        //createLaoMessage

        alert("CreateLao message data content maybe invalid but should be correclty decoded")
        When("the request is parsed")
        val parsed = MessageDecoder.parseData(gm)

        Then("it should be of type JsonRpcRequestCreateLao")
        inside(parsed){
          case Left(createJsonRpc: JsonRpcRequestCreateLao) => {
            And("the message params of the JsonRpcRequestCreateLao should not be empty")
            createJsonRpc.getParamsMessage should be (defined)
            And("the parsed message corresponds to the sent one")
            createJsonRpc.getParamsMessage.get should equal (createLaoMessage)
            And("the decoded message data is non empty ")
            val optDecodedData =  createJsonRpc.getDecodedData
            optDecodedData should be (defined)
            And("is of correct type: CreateLao")
            optDecodedData.get shouldBe a [CreateLao]
          }
          case Right(_) => fail(s"The message data format should succeeds with a Left[JsonRpcRequestCreateLao] but was <$parsed>")
          case _ => fail(s"The message data format format yield an unexpected result <$parsed>")
        }
      }

  def testBadFormat =
      (gm: GraphMessage, _: Any) => {
        Given("a graph message of JsonRpcRequest with bad message data format")
        //gm
        When("the request is parsed")
        val parsed = MessageDecoder.parseData(gm)

        Then("it should fail with the correct type Right[PipelineError]")
        inside(parsed){
          case Right(e) => {
            e shouldBe a [PipelineError]
            And("report a correct error code")
            e.code should equal(ErrorCodes.INVALID_DATA.id)
          }
          case Left(_) => fail(s"parsed message should fail with Right[PipelineError] but was a Left: <$parsed>")
          case _ =>  fail(s"parsed message <$parsed> resulted in an unexpected type")
        }
      }

  behavior of ("Message decoder when processing/decoding...")
  "A valid rpc request with valid message data for a create lao" should "succeed" in
      withCreateLaoFixiture(CreateLaoExamples.createLao)(testGoodFormat)

  "A valid rpc request/message data format but non-valid createLao data --empty lao name" should "succeed" in
      withCreateLaoFixiture(CreateLaoExamples.laoCreateEmptyName)(testGoodFormat)

  "A valid rpc request/message data format but non-valid createLao --invalid id" should "succeed" in
      withCreateLaoFixiture(CreateLaoExamples.laoCreateIdInvalid)(testGoodFormat)

  "A valid rpc request but non-valid message data format createLao --missing param" should "fail" in
      withCreateLaoFixiture(CreateLaoExamples.laoCreateMissingParams)(testBadFormat)

  "A valid rpc request but non-valid message data format createLao --additional param" should "fail" in
      withCreateLaoFixiture(CreateLaoExamples.laoCreateAdditionalParam)(testBadFormat)

  // TODO: Add test for base64
  // laoCreateOrgNot64
  // laoCreateWitNot64
}
