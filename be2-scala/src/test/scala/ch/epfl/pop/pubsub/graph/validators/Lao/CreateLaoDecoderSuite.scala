package ch.epfl.pop.pubsub.graph.validator.lao

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.lao.CreateLao
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import ch.epfl.pop.pubsub.MessageRegistry
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, MessageDecoder, PipelineError}
import org.scalatest.{Assertion, GivenWhenThen, Inside}
import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatest.matchers.should.Matchers
import util.examples.lao.CreateLaoExamples

class CreateLaoDecoderSuite extends FlatSpec with Matchers with Inside with GivenWhenThen {

  def withCreateLaoFixiture(msg: Message)(testCode: (GraphMessage, Message) => Any): Unit = {
    val jsonReq = CreateLaoExamples.getJsonRequestFromMessage(msg)
    testCode(Right(jsonReq), msg)
  }

  def testGoodFormat: (GraphMessage, Message) => Assertion =
    (gm: GraphMessage, createLaoMessage: Message) => {
      alert("CreateLao message data content maybe invalid but should be correctly decoded")

      Given("a correct graph message of JsonRpcRequest")
      And("a createLao message")
      When("the request is parsed")
      val parsed = MessageDecoder.parseData(gm, MessageRegistry.apply())

      Then("it should be of type JsonRpcRequest")
      inside(parsed) {
        case Right(createJsonRpc: JsonRpcRequest) =>
          And("it should be of type create lao")
          createJsonRpc.getDecodedDataHeader should equal((ObjectType.LAO, ActionType.CREATE))

          And("the message params of the JsonRpcRequestCreateLao should not be empty")
          createJsonRpc.getParamsMessage should be(defined)

          And("the decoded message data is non empty ")
          val optDecodedData = createJsonRpc.getDecodedData
          optDecodedData should be(defined)

          And("is of correct type: CreateLao")
          val message = optDecodedData.get
          message shouldBe a[CreateLao]
          val laoData = message.asInstanceOf[CreateLao]

          And("lao has a valid name")
          laoData.name shouldNot be(null)

          And("the timestamp exists")
          laoData.creation shouldNot be(null)
          laoData.creation.time should be > 0L

          And("the organizer public key is base64")
          noException shouldBe thrownBy(laoData.organizer.base64Data.decodeToString())

          And("the witnesses points to a non null list")
          laoData.witnesses shouldNot be(null)
          alert(s"The witnesses list was ${laoData.witnesses}")

          And("the id is not null")
          laoData.id shouldNot be(null)
          noException shouldBe thrownBy(laoData.id.base64Data.decodeToString())
        case Left(_) => fail(s"The message data format should succeed with a Right[JsonRpcRequestCreateLao] but was <$parsed>")
        case _        => fail(s"The message data format format yielded an unexpected result <$parsed>")
      }
    }

  def testBadFormat: (GraphMessage, Any) => Assertion =
    (gm: GraphMessage, _: Any) => {
      Given("a graph message of JsonRpcRequest with bad message data format")
      // gm
      When("the request is parsed")
      val parsed = MessageDecoder.parseData(gm, MessageRegistry.apply())

      Then("it should fail with the correct type Left[PipelineError]")
      inside(parsed) {
        case Left(e) =>
          e shouldBe a[PipelineError]
          And("report a correct error code")
          e.code should equal(ErrorCodes.INVALID_DATA.id)
        case Right(_) => fail(s"parsed message should fail with Left[PipelineError] but was a Right: <$parsed>")
        case _       => fail(s"parsed message <$parsed> resulted with an unexpected type")
      }
    }

  behavior of ("CreateLao decoder when processing/decoding...")
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

  "A valid rpc request but non-valid message data format createLao --organizer pk not base64" should "fail" in
    withCreateLaoFixiture(CreateLaoExamples.laoCreateOrgNot64)(testBadFormat)

  "A valid rpc request but non-valid message data format createLao --witness pk not base64" should "fail" in
    withCreateLaoFixiture(CreateLaoExamples.laoCreateWitNot64)(testBadFormat)
}
