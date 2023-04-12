package ch.epfl.pop.decentralized

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.{AnyFunSuiteLike => FunSuiteLike}
import org.scalatest.matchers.should.Matchers
import util.examples.JsonRpcRequestExample

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class MonitorSuite extends TestKit(ActorSystem("MonitorSuiteActorSystem")) with FunSuiteLike with Matchers with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    // Stops the test actor system
    TestKit.shutdownActorSystem(system)
  }

  test("monitor should start scheduling heartbeats when told a server connected") {

    val testProbe = TestProbe()
    val monitorRef = system.actorOf(
      Monitor.props(testProbe.ref, PERIODIC_HEARTBEAT = 2.seconds, MESSAGE_DELAY = 1.seconds)
    )

    testProbe.send(monitorRef, ConnectionMediator.AtLeastOneServerConnected)
    testProbe.expectMsgType[HeartbeatGenerator.SendHeartbeat]
    testProbe.expectMsgType[HeartbeatGenerator.SendHeartbeat]
    testProbe.expectMsgType[HeartbeatGenerator.SendHeartbeat]
  }

  test("monitor should schedule single heartbeat when receiving a Right GraphMessage") {

    val testProbe = TestProbe()
    val monitorRef = system.actorOf(
      Monitor.props(testProbe.ref, PERIODIC_HEARTBEAT = 60.seconds, MESSAGE_DELAY = 1.seconds)
    )

    //Needed to tell monitor ConnectionMediatorRef
    testProbe.send(monitorRef, ConnectionMediator.AtLeastOneServerConnected)

    val sink = Monitor.sink(monitorRef)
    Source.single(Right(JsonRpcRequestExample.subscribeRpcRequest)).to(sink).run()
    testProbe.expectMsgType[HeartbeatGenerator.SendHeartbeat]

  }

  test("monitor should send heartbeats only when servers are connected") {

    val testProbe = TestProbe()
    val monitorRef = system.actorOf(
      Monitor.props(testProbe.ref, PERIODIC_HEARTBEAT = 1.seconds, MESSAGE_DELAY = 60.seconds)
    )

    //Needed to tell monitor ConnectionMediatorRef
    testProbe.send(monitorRef, ConnectionMediator.AtLeastOneServerConnected)

    //Wait for the first hearbeat then "disconnect" servers
    testProbe.expectMsgType[HeartbeatGenerator.SendHeartbeat]
    testProbe.send(monitorRef, ConnectionMediator.NoServerConnected)

    testProbe.expectNoMessage(5.seconds)

    // Connect a server and check for heartbeats again
    testProbe.send(monitorRef, ConnectionMediator.AtLeastOneServerConnected)

    testProbe.expectMsgType[HeartbeatGenerator.SendHeartbeat]
  }
}
