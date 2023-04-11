package ch.epfl.pop.pubsub.graph.handlers

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestProbe
import ch.epfl.pop.model.objects.Channel
import ch.epfl.pop.pubsub.{AskPatternConstants, ClientActor, PubSubMediator}
import org.scalatest.funsuite.{AnyFunSuite => FunSuite}
import org.scalatest.matchers.should.Matchers
import util.examples.JsonRpcRequestExample

import scala.concurrent.Await
import scala.util.{Failure, Success}

class ParamsWithChannelHandlerSuite extends FunSuite with Matchers with AskPatternConstants {

  implicit val system: ActorSystem = ActorSystem()

  test("Subscribe attempt should succeed") {
    val mockClientRef = TestProbe()(system)

    val rpcExample = JsonRpcRequestExample.subscribeRpcRequest
    val expectedAsk = ClientActor.SubscribeTo(rpcExample.getParams.channel)
    val pipelineOutput = Source.single(Right(rpcExample))
      .via(ParamsWithChannelHandler.subscribeHandler(mockClientRef.ref))
      .runWith(Sink.head)

    val channel = mockClientRef.expectMsg(expectedAsk).channel
    mockClientRef.reply(PubSubMediator.SubscribeToAck(channel))

    Await.ready(pipelineOutput, duration).value match {
      case Some(Success(graphMessageOut)) => graphMessageOut should equal(Right(rpcExample))
      case Some(Failure(exception))       => Matchers.fail(exception.getMessage)
      case _                              => Matchers.fail()
    }
  }

  test("Subscribe attempt should fail") {
    val mockClientRef = TestProbe()(system)
    val rpcExample = JsonRpcRequestExample.subscribeRpcRequest
    val expectedAsk = ClientActor.SubscribeTo(rpcExample.getParams.channel)
    val pipelineOutput = Source.single(Right(rpcExample))
      .via(ParamsWithChannelHandler.subscribeHandler(mockClientRef.ref))
      .runWith(Sink.head)

    val channel = mockClientRef.expectMsg(expectedAsk).channel
    val wrongChannel = Channel(channel.channel + "/blabla")
    mockClientRef.reply(PubSubMediator.SubscribeToAck(wrongChannel))

    Await.ready(pipelineOutput, duration).value match {
      case Some(Success(graphMessageOut)) => graphMessageOut.isLeft should equal(true)
      case Some(Failure(exception))       => Matchers.fail(exception.getMessage)
      case _                              => Matchers.fail()
    }
  }

  test("Unsubscribe attempt should succeed") {
    val mockClientRef = TestProbe()(system)
    val rpcExample = JsonRpcRequestExample.unSubscribeRpcRequest
    val expectedAsk = ClientActor.UnsubscribeFrom(rpcExample.getParams.channel)
    val pipelineOutput = Source.single(Right(rpcExample))
      .via(ParamsWithChannelHandler.unsubscribeHandler(mockClientRef.ref))
      .runWith(Sink.head)

    val channel = mockClientRef.expectMsg(expectedAsk).channel
    mockClientRef.reply(PubSubMediator.UnsubscribeFromAck(channel))

    Await.ready(pipelineOutput, duration).value match {
      case Some(Success(graphMessageOut)) => graphMessageOut should equal(Right(rpcExample))
      case Some(Failure(exception))       => Matchers.fail(exception.getMessage)
      case _                              => Matchers.fail()
    }
  }

  test("Unsubscribe attempt should fail") {
    val mockClientRef = TestProbe()(system)
    val rpcExample = JsonRpcRequestExample.unSubscribeRpcRequest
    val expectedAsk = ClientActor.UnsubscribeFrom(rpcExample.getParams.channel)
    val pipelineOutput = Source.single(Right(rpcExample))
      .via(ParamsWithChannelHandler.unsubscribeHandler(mockClientRef.ref))
      .runWith(Sink.head)

    val channel = mockClientRef.expectMsg(expectedAsk).channel
    val wrongChannel = Channel(channel.channel + "/blabla")
    mockClientRef.reply(PubSubMediator.UnsubscribeFromAck(wrongChannel))

    Await.ready(pipelineOutput, duration).value match {
      case Some(Success(graphMessageOut)) => graphMessageOut.isLeft should equal(true)
      case Some(Failure(exception))       => Matchers.fail(exception.getMessage)
      case _                              => Matchers.fail()
    }
  }

}
