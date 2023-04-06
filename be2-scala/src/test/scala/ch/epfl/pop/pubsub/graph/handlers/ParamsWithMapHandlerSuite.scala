package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.stream.SinkShape
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.testkit.TestKit
import ch.epfl.pop.decentralized.ToyDbActor
import ch.epfl.pop.model.network.{JsonRpcRequest, MethodType}
import ch.epfl.pop.model.network.method.{GetMessagesById, Heartbeat}
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.{Base64Data, Channel, Hash}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.GraphMessage
import org.scalatest.funsuite.{AnyFunSuite, AnyFunSuiteLike}
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, equal}

import scala.collection.{immutable, mutable}
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
class ParamsWithMapHandlerSuite extends TestKit(ActorSystem("HbActorSuiteActorSystem")) with AnyFunSuiteLike with AskPatternConstants{

  final val toyDbActorRef: AskableActorRef = system.actorOf(Props(new ToyDbActor))
  final val boxUnderTest: Flow[GraphMessage, GraphMessage, NotUsed] = ParamsWithMapHandler.graph(toyDbActorRef)
  final val rpc: String = "rpc"
  final val id: Option[Int] = Some(0)

  // defining the channels
  final val CHANNEL1_NAME: String = "/root/wex/lao1Id"
  final val CHANNEL2_NAME: String = "/root/wex/lao2Id"
  final val CHANNEL1 = new Channel(CHANNEL1_NAME)
  final val CHANNEL2 = new Channel(CHANNEL2_NAME)

  //defining the messages
  final val MESSAGE1_ID: Hash = Hash(Base64Data.encode("message1Id"))
  final val MESSAGE2_ID: Hash = Hash(Base64Data.encode("message2Id"))
  final val MESSAGE3_ID: Hash = Hash(Base64Data.encode("message3Id"))
  final val MESSAGE4_ID: Hash = Hash(Base64Data.encode("message4Id"))
  final val MESSAGE5_ID: Hash = Hash(Base64Data.encode("message5Id"))
  final val MESSAGE1: Message = Message(null, null, null, MESSAGE1_ID, null, null)
  final val MESSAGE2: Message = Message(null, null, null, MESSAGE2_ID, null, null)
  final val MESSAGE3: Message = Message(null, null, null, MESSAGE3_ID, null, null)
  final val MESSAGE4: Message = Message(null, null, null, MESSAGE4_ID, null, null)
  final val MESSAGE5: Message = Message(null, null, null, MESSAGE5_ID, null, null)

  // defining a received heartbeat
  final val RECEIVED_HEART_BEAT_PARAMS = Map(CHANNEL1 -> Set(MESSAGE1_ID, MESSAGE2_ID, MESSAGE3_ID), CHANNEL2 -> Set(MESSAGE4_ID, MESSAGE5_ID))
  final val RECEIVED_HEARTBEAT: Heartbeat = Heartbeat(RECEIVED_HEART_BEAT_PARAMS)
  final val VALID_RECEIVED_HEARTBEAT_RPC: JsonRpcRequest = JsonRpcRequest(rpc, MethodType.HEARTBEAT, RECEIVED_HEARTBEAT, id)

  // defining what the answer to the received heartbeat should be
  final val EXPECTED_MISSING_MESSAGE_IDS = Map(CHANNEL1 -> Set(MESSAGE2_ID, MESSAGE3_ID), CHANNEL2 -> Set(MESSAGE5_ID))
  final val EXPECTED_GET_MSGS_BY_ID_RESPONSE: GetMessagesById = GetMessagesById(EXPECTED_MISSING_MESSAGE_IDS)
  final val EXPECTED_GET_MSGS_BY_ID_RPC: JsonRpcRequest = JsonRpcRequest(rpc, MethodType.GET_MESSAGES_BY_ID, EXPECTED_GET_MSGS_BY_ID_RESPONSE, id)


  test("sending a heartbeat correctly returns the missing ids") {
    val input: List[GraphMessage] = List(Right(VALID_RECEIVED_HEARTBEAT_RPC))
    val expectedOutput: List[GraphMessage] = List(Right(EXPECTED_GET_MSGS_BY_ID_RPC))
    val source = Source(input)
    val s = source.via(boxUnderTest).runWith(Sink.seq[GraphMessage])
    Await.ready(s, duration).value match {
      case Some(Success(seq)) => seq.toList.head match {
        case Right(jsonRpcReq : JsonRpcRequest) => jsonRpcReq.getParams.asInstanceOf[GetMessagesById].channelsToMessageIds should equal(EXPECTED_MISSING_MESSAGE_IDS)
        case _ => 1 should equal(0)
      }


      case _ => 1 should equal(0)
    }
  }

}
