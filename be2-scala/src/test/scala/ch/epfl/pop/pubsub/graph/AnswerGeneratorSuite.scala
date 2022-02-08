package ch.epfl.pop.pubsub.graph

import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse, ResultObject}
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.pubsub.graph.validators.SchemaValidatorSuite._
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import util.examples.MessageExample

import scala.concurrent.duration.FiniteDuration

class AnswerGeneratorSuite extends TestKit(ActorSystem("Test")) with FunSuiteLike with ImplicitSender with Matchers with BeforeAndAfterAll {

  final val pathCatchupJson = "../protocol/examples/query/catchup/catchup.json"
  final val pathPublishJson = "../protocol/examples/query/publish/publish.json"
  final val pathBroadcastJson = "../protocol/examples/query/broadcast/broadcast.json"

  // Implicites for system actors
  implicit val duration = FiniteDuration(5, "seconds")
  implicit val timeout = Timeout(duration)

  override def afterAll(): Unit = {
    // Stops the testKit
    TestKit.shutdownActorSystem(system)
  }

  /**
   * Creates and spawns a mocked version of
   * DBactor with special behavior when receiving Catchup
   *
   * @param msgs messages to send back as result
   * @return: Askable mocked Dbactor
   */
  def mockDbWithMessages(msgs: List[Message]): AskableActorRef = {
    val mockedDB = Props(new Actor() {
      override def receive = {
        case DbActor.Catchup(channel) =>
          sender() ! DbActor.DbActorCatchupAck(msgs)
      }
    }
    )
    system.actorOf(mockedDB)
  }

  /**
   * Creates and spawns with NAck response for a
   * catchup
   *
   * @param code        : Error code
   * @param description : a brief desciption of the error
   * @return Askable mocked Dbactor
   */
  def mockDbWithNack(code: Int, description: String): AskableActorRef = {
    val mockedDB = Props(new Actor() {
      override def receive = {
        case DbActor.Catchup(_) =>
          sender() ! DbActor.DbActorNAck(code, description)
      }
    }
    )
    system.actorOf(mockedDB)
  }

  def getJsonRPC(path: String): JsonRpcRequest = {
    val jsonStr = getJsonStringFromFile(path)
    val rpcRequest: JsonRpcRequest = JsonRpcRequest.buildFromJson(jsonStr)
    rpcRequest
  }

  test("Publish: correct response test") {

    val rpcPublishReq = getJsonRPC(pathPublishJson)
    val message: GraphMessage = AnswerGenerator.generateAnswer(Left(rpcPublishReq))
    val expected: GraphMessage = Left(JsonRpcResponse(
      RpcValidator.JSON_RPC_VERSION, Some(new ResultObject(0)), None, rpcPublishReq.id))

    message should be(expected)
  }

  lazy val rpcCatchupReq = getJsonRPC(pathCatchupJson)
  test("Catchup: correct response test for Nil Messages") {

    lazy val dbActorRef = mockDbWithMessages(Nil)
    val message: GraphMessage = new AnswerGenerator(dbActorRef).generateAnswer(Left(rpcCatchupReq))

    def resultObject: ResultObject = new ResultObject(Nil)

    val expected = Left(JsonRpcResponse(
      RpcValidator.JSON_RPC_VERSION, Some(resultObject), None, rpcCatchupReq.id))

    message should be(expected)
    system.stop(dbActorRef.actorRef)
  }

  test("Catchup: correct response test for one Message") {

    val messages = MessageExample.MESSAGE :: Nil
    lazy val dbActorRef = mockDbWithMessages(messages)
    val gmsg: GraphMessage = new AnswerGenerator(dbActorRef).generateAnswer(Left(rpcCatchupReq))

    def resultObject: ResultObject = new ResultObject(messages)

    val expected = Left(JsonRpcResponse(
      RpcValidator.JSON_RPC_VERSION, Some(resultObject), None, rpcCatchupReq.id))

    gmsg should be(expected)
    system.stop(dbActorRef.actorRef)
  }

  test("Catchup: error on non existing channel test") {

    lazy val dbActorRef =
      mockDbWithNack(ErrorCodes.INVALID_RESOURCE.id,
        "Database cannot catchup from a channel that does not exist in db")

    val gmsg: GraphMessage = new AnswerGenerator(dbActorRef).generateAnswer(Left(rpcCatchupReq))
    val expected = Right(PipelineError(ErrorCodes.INVALID_RESOURCE.id, "Database cannot catchup from a channel that does not exist in db", rpcCatchupReq.id))

    gmsg should be(expected)
    system.stop(dbActorRef.actorRef)
  }

  test("Broadcast: answer error on brodcast") {

    val rpcBroadcastReq = getJsonRPC(pathBroadcastJson)
    val message: GraphMessage = AnswerGenerator.generateAnswer(Left(rpcBroadcastReq))

    message shouldBe a[Right[_, PipelineError]]
    message.toOption.isDefined should be(true)
    message.toOption.get.code should be(ErrorCodes.SERVER_ERROR.id)
  }

  test("Convert Right Pipeline messages into Error Messages test") {

    val optid = Option(1)
    val perror = PipelineError(
      ErrorCodes.SERVER_ERROR.id,
      "Server received a Broadcast message which should never happen (broadcast messages are only emitted by server)",
      optid
    )
    val gmsg = AnswerGenerator.generateAnswer(Right(perror))
    gmsg shouldBe a[Left[JsonRpcResponse, _]]
    val msg = gmsg.swap.toOption
    msg.isDefined should be(true)
    msg.get.asInstanceOf[JsonRpcResponse].jsonrpc should be(RpcValidator.JSON_RPC_VERSION)
    msg.get.asInstanceOf[JsonRpcResponse].id should be(optid)
  }
}
