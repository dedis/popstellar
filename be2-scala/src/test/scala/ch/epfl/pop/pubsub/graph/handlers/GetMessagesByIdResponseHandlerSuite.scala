package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.testkit.{TestKit, TestProbe}
import ch.epfl.pop.model.network.{JsonRpcResponse, ResultObject}
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.{Base64Data, Channel, DbActorNAckException, Hash}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.GraphMessage
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.storage.DbActor
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.concurrent.Await

class GetMessagesByIdResponseHandlerSuite extends TestKit(ActorSystem("GetMessagesByIdResponseHandlerSuiteSystem")) with AnyFunSuiteLike with AskPatternConstants {

  class TestDb(testProbe: ActorRef) extends Actor {
    override def receive: Receive = {
      case DbActor.Write(channel, message) =>
        testProbe ! (channel, message)
    }
  }
  class FailingTestDb(testProbe: ActorRef) extends Actor {
    override def receive: Receive = {
      case DbActor.Write(channel, message) =>
        testProbe ! (channel, message)
        throw(DbActorNAckException(0,""))
    }
  }

  val testProbe = TestProbe()
  final val CHANNEL1: Channel = Channel("/root/wex/lao1Id")
  final val CHANNEL2: Channel = Channel("/root/wex/lao2Id")
  final val MESSAGE1_ID: Hash = Hash(Base64Data.encode("message1Id"))
  final val MESSAGE2_ID: Hash = Hash(Base64Data.encode("message2Id"))
  final val MESSAGE1: Message = Message(null, null, null, MESSAGE1_ID, null, null)
  final val MESSAGE2: Message = Message(null, null, null, MESSAGE2_ID, null, null)
  final val missingMessages = Map((CHANNEL1, Set(MESSAGE1)), (CHANNEL2, Set(MESSAGE2)))
  final val receivedResponse: JsonRpcResponse = JsonRpcResponse(RpcValidator.JSON_RPC_VERSION, new ResultObject(missingMessages), None)

  test("receiving a get messages by id response sends a write message to the data base") {
    val boxUnderTest: Flow[GraphMessage, GraphMessage, NotUsed] = GetMessagesByIdResponseHandler.graph(system.actorOf(Props(new TestDb(testProbe.ref))))
    val input: List[GraphMessage] = List(Right(receivedResponse))
    val source = Source(input)
    val s = source.via(boxUnderTest).runWith(Sink.seq[GraphMessage])
    Await.ready(s, duration)
    testProbe.expectMsgAllOf((CHANNEL1, MESSAGE1), (CHANNEL2, MESSAGE2))
  }


  test("failing to write a get messages by id response in the data base retries exactly three times before giving up") {
    val boxUnderTest: Flow[GraphMessage, GraphMessage, NotUsed] = GetMessagesByIdResponseHandler.graph(system.actorOf(Props(new FailingTestDb(testProbe.ref))))
    val input: List[GraphMessage] = List(Right(receivedResponse))
    val source = Source(input)
    val s = source.via(boxUnderTest).runWith(Sink.seq[GraphMessage])
    Await.ready(s, duration)
    testProbe.expectMsgAllOf((CHANNEL1, MESSAGE1),(CHANNEL1, MESSAGE1),(CHANNEL1, MESSAGE1),(CHANNEL2, MESSAGE2),(CHANNEL2, MESSAGE2),(CHANNEL2, MESSAGE2))
  }
}
