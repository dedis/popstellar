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
      Monitor.props(testProbe.ref, heartbeatRate = 2.seconds, messageDelay = 1.seconds)
    )

    testProbe.send(monitorRef, Monitor.AtLeastOneServerConnected)
    testProbe.expectMsgType[Monitor.GenerateAndSendHeartbeat]
    testProbe.expectMsgType[Monitor.GenerateAndSendHeartbeat]
    testProbe.expectMsgType[Monitor.GenerateAndSendHeartbeat]
  }

  test("monitor should schedule single heartbeat when receiving a Right GraphMessage") {

    val testProbe = TestProbe()
    val monitorRef = system.actorOf(
      Monitor.props(testProbe.ref, heartbeatRate = 60.seconds, messageDelay = 1.seconds)
    )

    // Needed to tell monitor ConnectionMediatorRef
    testProbe.send(monitorRef, Monitor.AtLeastOneServerConnected)

    val sink = Monitor.sink(monitorRef)
    Source.single(Right(JsonRpcRequestExample.subscribeRpcRequest)).to(sink).run()
    testProbe.expectMsgType[Monitor.GenerateAndSendHeartbeat]

  }

  test("monitor should send heartbeats only when servers are connected") {

    val testProbe = TestProbe()
    val monitorRef = system.actorOf(
      Monitor.props(testProbe.ref, heartbeatRate = 1.seconds, messageDelay = 60.seconds)
    )

    // Needed to tell monitor ConnectionMediatorRef
    testProbe.send(monitorRef, Monitor.AtLeastOneServerConnected)

    // Wait for the first heartbeat then "disconnect" servers
    testProbe.expectMsgType[Monitor.GenerateAndSendHeartbeat]
    testProbe.send(monitorRef, Monitor.NoServerConnected)

    testProbe.expectNoMessage(5.seconds)

    // Connect a server and check for heartbeats again
    testProbe.send(monitorRef, Monitor.AtLeastOneServerConnected)

    testProbe.expectMsgType[Monitor.GenerateAndSendHeartbeat]
  }
}
