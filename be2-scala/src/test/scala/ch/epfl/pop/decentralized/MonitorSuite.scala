package ch.epfl.pop.decentralized

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.testkit.{TestKit, TestProbe}
import ch.epfl.pop.model.network.method.ParamsWithMap
import ch.epfl.pop.model.network.{JsonRpcRequest, MethodType}
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.{AnyFunSuiteLike => FunSuiteLike}
import org.scalatest.matchers.should.Matchers
import util.examples.JsonRpcRequestExample
import scala.concurrent.duration.DurationInt

class MonitorSuite extends TestKit(ActorSystem("MonitorSuiteActorSystem")) with FunSuiteLike with Matchers with BeforeAndAfterAll {

  private val fastRate = 1.seconds
  private val slowRate = 60.seconds
  private val timeout = 3.seconds

  override def afterAll(): Unit = {
    // Stops the test actor system
    TestKit.shutdownActorSystem(system)
  }

  test("monitor should start scheduling heartbeats when told a server connected") {

    val testProbe = TestProbe()
    val monitorRef = system.actorOf(
      Monitor.props(testProbe.ref, heartbeatRate = fastRate, messageDelay = fastRate)
    )

    testProbe.send(monitorRef, Monitor.AtLeastOneServerConnected)
    testProbe.expectMsgType[Monitor.GenerateAndSendHeartbeat](timeout)
    testProbe.expectMsgType[Monitor.GenerateAndSendHeartbeat](timeout)
    testProbe.expectMsgType[Monitor.GenerateAndSendHeartbeat](timeout)
  }

  test("monitor should schedule single heartbeat when receiving a Right GraphMessage") {

    val heartbeat = JsonRpcRequest(RpcValidator.JSON_RPC_VERSION, MethodType.HEARTBEAT, new ParamsWithMap(Map.empty), None)
    val getMessagesById = JsonRpcRequest(RpcValidator.JSON_RPC_VERSION, MethodType.GET_MESSAGES_BY_ID, new ParamsWithMap(Map.empty), None)

    val testProbe = TestProbe()
    val monitorRef = system.actorOf(
      Monitor.props(testProbe.ref, heartbeatRate = slowRate, messageDelay = fastRate)
    )

    // Needed to tell monitor ConnectionMediatorRef
    val sink = Monitor.sink(monitorRef)
    testProbe.send(monitorRef, Monitor.AtLeastOneServerConnected)

    Source.single(Right(heartbeat)).to(sink).run()
    testProbe.expectNoMessage(timeout)

    Source.single(Right(JsonRpcRequestExample.subscribeRpcRequest)).to(sink).run()
    testProbe.expectMsgType[Monitor.GenerateAndSendHeartbeat](timeout)

    Source.single(Right(getMessagesById)).to(sink).run()
    testProbe.expectNoMessage(timeout)
  }

  test("monitor should send heartbeats only when servers are connected") {

    val testProbe = TestProbe()
    val monitorRef = system.actorOf(
      Monitor.props(testProbe.ref, heartbeatRate = fastRate, messageDelay = slowRate)
    )

    // Needed to tell monitor ConnectionMediatorRef
    testProbe.send(monitorRef, Monitor.AtLeastOneServerConnected)

    // Wait for the first heartbeat then "disconnect" servers
    testProbe.expectMsgType[Monitor.GenerateAndSendHeartbeat](timeout)
    testProbe.send(monitorRef, Monitor.NoServerConnected)

    testProbe.expectNoMessage(timeout)

    // Connect a server and check for heartbeats again
    testProbe.send(monitorRef, Monitor.AtLeastOneServerConnected)

    testProbe.expectMsgType[Monitor.GenerateAndSendHeartbeat](timeout)
  }
}
