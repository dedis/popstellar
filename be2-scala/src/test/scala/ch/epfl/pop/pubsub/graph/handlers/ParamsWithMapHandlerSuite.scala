package ch.epfl.pop.pubsub.graph.handlers

import akka.actor.{ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import ch.epfl.pop.decentralized.ToyDbActor
import ch.epfl.pop.model.network.method.GetMessagesById
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.GraphMessage
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, equal}
import util.examples.JsonRpcRequestExample._

import scala.concurrent.Await
import scala.util.Success
class ParamsWithMapHandlerSuite extends TestKit(ActorSystem("HbActorSuiteActorSystem")) with AnyFunSuiteLike with AskPatternConstants {

  final val toyDbActorRef: AskableActorRef = system.actorOf(Props(new ToyDbActor))
  final val heartbeatHandler = ParamsWithMapHandler.heartbeatHandler(toyDbActorRef)
  final val getMessagesByIdHandler = ParamsWithMapHandler.getMessagesByIdHandler(toyDbActorRef)
  final val rpc: String = "rpc"
  final val id: Option[Int] = Some(0)

  test("sending a heartbeat correctly returns the missing ids") {
    val input: List[GraphMessage] = List(Right(VALID_RECEIVED_HEARTBEAT_RPC))
    val source = Source(input)
    val s = source.via(heartbeatHandler).runWith(Sink.seq[GraphMessage])
    Await.ready(s, duration).value match {
      case Some(Success(seq)) => seq.toList.head match {
          case Right(jsonRpcReq: JsonRpcRequest) => jsonRpcReq.getParams.asInstanceOf[GetMessagesById].channelsToMessageIds should equal(EXPECTED_MISSING_MESSAGE_IDS)
          case _                                 => 1 should equal(0)
        }

      case _ => 1 should equal(0)
    }
  }

  test("sending a getMessagesById correctly returns the missing messages") {
    val input: List[GraphMessage] = List(Right(VALID_RECEIVED_GET_MSG_BY_ID_RPC))
    val source = Source(input)
    val s = source.via(getMessagesByIdHandler).runWith(Sink.seq[GraphMessage])
    Await.ready(s, duration).value match {
      case Some(Success(seq)) => seq.toList.head match {
          case Right(jsonRpcResp: JsonRpcResponse) => jsonRpcResp.result.get.resultMap.get should equal(EXPECTED_MISSING_MESSAGES)
          case _                                   => 1 should equal(0)
        }
      case _ => 1 should equal(0)
    }

  }

  test("receiving a heartbeat with unknown channel asks back for this channel") {
    val input: List[GraphMessage] = List(Right(VALID_RECEIVED_UNKNOWN_CHANNEL_HEARTBEAT_RPC))
    val source = Source(input)
    val s = source.via(heartbeatHandler).runWith(Sink.seq[GraphMessage])
    Await.ready(s, duration).value match {
      case Some(Success(seq)) => seq.toList.head match {
          case Right(jsonRpcReq: JsonRpcRequest) => jsonRpcReq.getParams.asInstanceOf[GetMessagesById].channelsToMessageIds should equal(EXPECTED_UNKNOWN_CHANNEL_MISSING_MESSAGE_IDS)
          case _                                 => 1 should equal(0)
        }

      case _ => 1 should equal(0)
    }
  }

}
