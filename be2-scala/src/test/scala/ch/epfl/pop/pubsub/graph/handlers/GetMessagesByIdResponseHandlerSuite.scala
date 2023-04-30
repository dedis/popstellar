package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.actor.{Actor, ActorRef, ActorSystem, Props, Status}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.testkit.{TestKit, TestProbe}
import ch.epfl.pop.model.network.{JsonRpcResponse, ResultObject}
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.{Base64Data, Channel, DbActorNAckException, Hash}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage}
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.storage.DbActor
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.concurrent.Await

class GetMessagesByIdResponseHandlerSuite extends TestKit(ActorSystem("GetMessagesByIdResponseHandlerSuiteSystem")) with AnyFunSuiteLike with AskPatternConstants {

  class TestDb(testProbe: ActorRef) extends Actor {
    override def receive: Receive = {
      case DbActor.WriteAndPropagate(channel, message) =>
        testProbe ! (channel, message)
        sender() ! DbActor.DbActorAck()
    }
  }
  class FailingTestDb(testProbe: ActorRef) extends Actor {
    override def receive: Receive = {
      case DbActor.WriteAndPropagate(channel, message) =>
        testProbe ! (channel, message)
        sender() ! Status.Failure(DbActorNAckException(ErrorCodes.INVALID_ACTION.id,
          s"channel '$channel' does not exist in db"))
    }
  }

  class FailingThenSucceedingTestDb(testProbe: ActorRef) extends Actor {

    def counter(numberOfTrials: Int): Receive = {
      case DbActor.WriteAndPropagate(channel, message) =>
        if (numberOfTrials == 0) {
          testProbe ! (channel, message)
          sender() ! Status.Failure(DbActorNAckException(ErrorCodes.INVALID_ACTION.id,
            s"channel '$channel' does not exist in db"))
          context.become(counter(numberOfTrials + 1))
        } else {
          testProbe ! (channel, message)
          sender() ! DbActor.DbActorAck()
          context.become(counter(0))
        }
    }
    def receive: Receive = counter(0)
  }

  private val testProbe = TestProbe()
  final val CHANNEL1: Channel = Channel("/root/wex/lao1Id")
  final val CHANNEL2: Channel = Channel("/root/wex/lao2Id")
  final val MESSAGE1_ID: Hash = Hash(Base64Data.encode("message1Id"))
  final val MESSAGE2_ID: Hash = Hash(Base64Data.encode("message2Id"))
  final val MESSAGE1: Message = Message(null, null, null, MESSAGE1_ID, null, null)
  final val MESSAGE2: Message = Message(null, null, null, MESSAGE2_ID, null, null)
  final val missingMessages = Map((CHANNEL1, Set(MESSAGE1)), (CHANNEL2, Set(MESSAGE2)))
  final val receivedResponse: JsonRpcResponse = JsonRpcResponse(RpcValidator.JSON_RPC_VERSION, new ResultObject(missingMessages), None)

  test("by receiving a get_messages_by_id response, it sends a write message to the database") {
    val boxUnderTest: Flow[GraphMessage, GraphMessage, NotUsed] = GetMessagesByIdResponseHandler.graph(system.actorOf(Props(new TestDb(testProbe.ref))))
    val input: List[GraphMessage] = List(Right(receivedResponse))
    val source = Source(input)
    val s = source.via(boxUnderTest).runWith(Sink.seq[GraphMessage])
    Await.ready(s, duration)
    testProbe.expectMsgAllOf((CHANNEL1, MESSAGE1), (CHANNEL2, MESSAGE2))
    testProbe.expectNoMessage()
  }

  test("by failing to write a  get_messages_by_id response in the database, it retries exactly three times before giving up") {
    val boxUnderTest: Flow[GraphMessage, GraphMessage, NotUsed] = GetMessagesByIdResponseHandler.graph(system.actorOf(Props(new FailingTestDb(testProbe.ref))))
    val input: List[GraphMessage] = List(Right(receivedResponse))
    val source = Source(input)
    val s = source.via(boxUnderTest).runWith(Sink.seq[GraphMessage])
    Await.ready(s, duration)
    testProbe.expectMsg((CHANNEL1, MESSAGE1))
    testProbe.expectMsg((CHANNEL1, MESSAGE1))
    testProbe.expectMsg((CHANNEL1, MESSAGE1))
    testProbe.expectMsg((CHANNEL2, MESSAGE2))
    testProbe.expectMsg((CHANNEL2, MESSAGE2))
    testProbe.expectMsg((CHANNEL2, MESSAGE2))
    testProbe.expectNoMessage()
  }

  test("by succeeding to write on the db, it doesn't retry to write on it") {
    val boxUnderTest: Flow[GraphMessage, GraphMessage, NotUsed] = GetMessagesByIdResponseHandler.graph(system.actorOf(Props(new FailingThenSucceedingTestDb(testProbe.ref))))
    val input: List[GraphMessage] = List(Right(receivedResponse))
    val source = Source(input)
    val s = source.via(boxUnderTest).runWith(Sink.seq[GraphMessage])
    Await.ready(s, duration)
    testProbe.expectMsg((CHANNEL1, MESSAGE1))
    testProbe.expectMsg((CHANNEL1, MESSAGE1))
    testProbe.expectMsg((CHANNEL2, MESSAGE2))
    testProbe.expectMsg((CHANNEL2, MESSAGE2))
    testProbe.expectNoMessage()
  }
}
