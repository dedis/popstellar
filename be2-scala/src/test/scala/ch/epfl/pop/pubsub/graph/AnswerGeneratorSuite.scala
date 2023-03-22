package ch.epfl.pop.pubsub.graph

import akka.actor.{Actor, ActorSystem, Props, Status}
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse, ResultObject}
import ch.epfl.pop.model.objects.DbActorNAckException
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.pubsub.graph.validators.SchemaVerifierSuite._
import ch.epfl.pop.storage.DbActor
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.{AnyFunSuiteLike => FunSuiteLike}
import org.scalatest.matchers.should.Matchers
import util.examples.MessageExample

import scala.concurrent.duration.FiniteDuration

class AnswerGeneratorSuite extends TestKit(ActorSystem("Test")) with FunSuiteLike with ImplicitSender with Matchers with BeforeAndAfterAll {

  final val pathCatchupJson = "../protocol/examples/query/catchup/catchup.json"
  final val pathPublishJson = "../protocol/examples/query/publish/publish.json"
  final val pathBroadcastJson = "../protocol/examples/query/broadcast/broadcast.json"

  // Implicits for system actors
  implicit val duration: FiniteDuration = FiniteDuration(5, "seconds")
  implicit val timeout: Timeout = Timeout(duration)

  override def afterAll(): Unit = {
    // Stops the testKit
    TestKit.shutdownActorSystem(system)
  }

  /** Creates and spawns a mocked version of DbActor with special behavior when receiving Catchup
    *
    * @param messages
    *   messages to send back as result
    * @return
    *   Askable mocked DbActor
    */
  def mockDbWithMessages(messages: List[Message]): AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.Catchup(_) =>
          sender() ! DbActor.DbActorCatchupAck(messages)
      }
    })
    system.actorOf(dbActorMock)
  }

  /** Creates and spawns with NAck response for a catchup
    *
    * @param code
    *   error code
    * @param description
    *   a brief description of the error
    * @return
    *   Askable mocked DbActor
    */
  def mockDbWithNack(code: Int, description: String): AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.Catchup(_) =>
          sender() ! Status.Failure(DbActorNAckException(code, description))
      }
    })
    system.actorOf(dbActorMock)
  }

  def getJsonRPC(path: String): JsonRpcRequest = {
    val jsonStr = getJsonStringFromFile(path)
    val rpcRequest: JsonRpcRequest = JsonRpcRequest.buildFromJson(jsonStr)
    rpcRequest
  }

  test("Publish: correct response test") {

    val rpcPublishReq = getJsonRPC(pathPublishJson)
    val message: GraphMessage = AnswerGenerator.generateAnswer(Right(rpcPublishReq))
    val expected: GraphMessage = Right(JsonRpcResponse(
      RpcValidator.JSON_RPC_VERSION,
      Some(new ResultObject(0)),
      None,
      rpcPublishReq.id
    ))

    message should be(expected)
  }

  lazy val rpcCatchupReq: JsonRpcRequest = getJsonRPC(pathCatchupJson)
  test("Catchup: correct response test for Nil Messages") {

    lazy val dbActorRef = mockDbWithMessages(Nil)
    val message: GraphMessage = new AnswerGenerator(dbActorRef).generateAnswer(Right(rpcCatchupReq))

    def resultObject: ResultObject = new ResultObject(Nil)

    val expected = Right(JsonRpcResponse(
      RpcValidator.JSON_RPC_VERSION,
      Some(resultObject),
      None,
      rpcCatchupReq.id
    ))

    message should be(expected)
    system.stop(dbActorRef.actorRef)
  }

  test("Catchup: correct response test for one Message") {

    val messages = MessageExample.MESSAGE :: Nil
    lazy val dbActorRef = mockDbWithMessages(messages)
    val gmsg: GraphMessage = new AnswerGenerator(dbActorRef).generateAnswer(Right(rpcCatchupReq))

    def resultObject: ResultObject = new ResultObject(messages)

    val expected = Right(JsonRpcResponse(
      RpcValidator.JSON_RPC_VERSION,
      Some(resultObject),
      None,
      rpcCatchupReq.id
    ))

    gmsg should be(expected)
    system.stop(dbActorRef.actorRef)
  }

  test("Catchup: error on non existing channel test") {

    lazy val dbActorRef =
      mockDbWithNack(ErrorCodes.INVALID_RESOURCE.id, "error (mock)")

    val gmsg: GraphMessage = new AnswerGenerator(dbActorRef).generateAnswer(Right(rpcCatchupReq))
    val expected = Left(PipelineError(ErrorCodes.INVALID_RESOURCE.id, "AnswerGenerator failed : error (mock)", rpcCatchupReq.id))

    gmsg should be(expected)
    system.stop(dbActorRef.actorRef)
  }

  test("Broadcast: answer error on broadcast") {

    val rpcBroadcastReq = getJsonRPC(pathBroadcastJson)
    val message: GraphMessage = AnswerGenerator.generateAnswer(Right(rpcBroadcastReq))

    message shouldBe a[Left[PipelineError, _]]
    message.isLeft should be(true)
    message.swap.toOption.get.code should be(ErrorCodes.SERVER_ERROR.id)
  }

  test("Convert Left Pipeline messages into Error Messages test") {

    val optId = Option(1)
    val error = PipelineError(
      ErrorCodes.SERVER_ERROR.id,
      "Server received a Broadcast message which should never happen (broadcast messages are only emitted by server)",
      optId
    )
    val gm = AnswerGenerator.generateAnswer(Left(error))
    gm shouldBe a[Right[_, JsonRpcResponse]]
    val msg = gm.toOption
    msg.isDefined should be(true)
    msg.get.asInstanceOf[JsonRpcResponse].jsonrpc should be(RpcValidator.JSON_RPC_VERSION)
    msg.get.asInstanceOf[JsonRpcResponse].id should be(optId)
  }
}
