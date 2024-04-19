package ch.epfl.pop.decentralized

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.stream.scaladsl.Source
import akka.testkit.{TestKit, TestProbe}
import ch.epfl.pop.config.RuntimeEnvironment.serverPeersListPath
import ch.epfl.pop.config.RuntimeEnvironmentTestingHelper.testWriteToServerPeersConfig
import ch.epfl.pop.model.network.method.{Heartbeat, ParamsWithMap}
import ch.epfl.pop.model.network.{JsonRpcRequest, MethodType}
import ch.epfl.pop.model.objects.{Base64Data, Channel, Hash}
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuiteLike as FunSuiteLike
import org.scalatest.matchers.should.Matchers
import util.examples.JsonRpcRequestExample
import ch.epfl.pop.storage.DbActor
import akka.pattern.ask

import java.io.{File, PrintWriter}
import java.nio.file.Path
import scala.collection.immutable.HashMap
import scala.concurrent.duration.DurationInt

class MonitorSuite extends TestKit(ActorSystem("MonitorSuiteActorSystem")) with FunSuiteLike with Matchers with BeforeAndAfterAll {

  private val fastRate = 1.seconds
  private val slowRate = 60.seconds
  private val timeout = 3.seconds

  final val CHANNEL1_NAME: String = "/root/wex/lao1Id"
  final val CHANNEL2_NAME: String = "/root/wex/lao2Id"
  final val CHANNEL1 = new Channel(CHANNEL1_NAME)
  final val CHANNEL2 = new Channel(CHANNEL2_NAME)

  final val MESSAGE1_ID: Hash = Hash(Base64Data.encode("message1Id"))
  final val MESSAGE2_ID: Hash = Hash(Base64Data.encode("message2Id"))
  final val MESSAGE3_ID: Hash = Hash(Base64Data.encode("message3Id"))
  final val MESSAGE4_ID: Hash = Hash(Base64Data.encode("message4Id"))
  final val MESSAGE5_ID: Hash = Hash(Base64Data.encode("message5Id"))

  final val toyDbActorRef: ActorRef = system.actorOf(Props(new ToyDbActor))
  final val failingToyDbActorRef: ActorRef = system.actorOf(Props(new FailingToyDbActor))

  override def afterAll(): Unit = {
    // Stops the test actor system
    TestKit.shutdownActorSystem(system)
  }

  test("monitor should start scheduling heartbeats when told a server connected") {
    val testProbe = TestProbe()
    val monitorRef = system.actorOf(
      Monitor.props(testProbe.ref, heartbeatRate = fastRate, messageDelay = fastRate)
    )

    testProbe.watch(monitorRef)

    testProbe.send(monitorRef, ConnectionMediator.Ping())
    testProbe.expectMsgType[ConnectionMediator.ConnectTo](timeout)
    testProbe.send(monitorRef, Monitor.AtLeastOneServerConnected)
    testProbe.expectMsgType[DbActor.GenerateHeartbeat](timeout)
    testProbe.reply(DbActor.DbActorGenerateHeartbeatAck(HashMap()))

    monitorRef ! PoisonPill
    testProbe.expectTerminated(monitorRef)
  }

  test("monitor should schedule single heartbeat when receiving a Right GraphMessage") {
    val testProbe = TestProbe()
    val monitorRef = system.actorOf(
      Monitor.props(testProbe.ref, heartbeatRate = slowRate, messageDelay = fastRate)
    )

    testProbe.watch(monitorRef)

    // Needed to tell monitor ConnectionMediatorRef
    val sink = Monitor.sink(monitorRef)
    testProbe.send(monitorRef, Monitor.AtLeastOneServerConnected)

    Source.single(Right(JsonRpcRequestExample.subscribeRpcRequest)).to(sink).run()
    testProbe.expectMsgType[DbActor.GenerateHeartbeat](timeout)
    testProbe.reply(DbActor.DbActorGenerateHeartbeatAck(HashMap()))

    monitorRef ! PoisonPill
    testProbe.expectTerminated(monitorRef)
  }

  test("monitor should not schedule any heartbeats when receiving a heartbeat or get_messages_by_id") {
    val heartbeat = JsonRpcRequest(RpcValidator.JSON_RPC_VERSION, MethodType.heartbeat, new ParamsWithMap(Map.empty), None)
    val getMessagesById = JsonRpcRequest(RpcValidator.JSON_RPC_VERSION, MethodType.get_messages_by_id, new ParamsWithMap(Map.empty), None)

    val testProbe = TestProbe()
    val monitorRef = system.actorOf(
      Monitor.props(testProbe.ref, heartbeatRate = slowRate, messageDelay = fastRate)
    )

    testProbe.watch(monitorRef)

    // Needed to tell monitor ConnectionMediatorRef
    val sink = Monitor.sink(monitorRef)
    testProbe.send(monitorRef, Monitor.AtLeastOneServerConnected)
    Source.single(Right(heartbeat)).to(sink).run()

    testProbe.expectNoMessage(timeout)

    // Check that monitor is still doing fine when receiving other Right(...)
    Source.single(Right(JsonRpcRequestExample.subscribeRpcRequest)).to(sink).run()
    testProbe.expectMsgType[DbActor.GenerateHeartbeat](timeout)
    testProbe.reply(DbActor.DbActorGenerateHeartbeatAck(HashMap()))

    Source.single(Right(getMessagesById)).to(sink).run()
    testProbe.expectNoMessage(timeout)

    monitorRef ! PoisonPill
    testProbe.expectTerminated(monitorRef)
  }

  test("monitor should send heartbeats if and only if servers are connected") {

    val testProbe = TestProbe()
    val monitorRef = system.actorOf(
      Monitor.props(testProbe.ref, heartbeatRate = fastRate, messageDelay = fastRate)
    )

    testProbe.watch(monitorRef)

    // Needed to tell monitor ConnectionMediatorRef
    testProbe.send(monitorRef, Monitor.AtLeastOneServerConnected)

    // Wait for the first heartbeat then "disconnect" servers
    testProbe.expectMsgType[DbActor.GenerateHeartbeat](timeout)
    testProbe.reply(DbActor.DbActorGenerateHeartbeatAck(HashMap()))
    testProbe.send(monitorRef, Monitor.NoServerConnected)
    testProbe.expectNoMessage(timeout)

    // No single heartbeat should be scheduled either
    Source.single(Right(JsonRpcRequestExample.subscribeRpcRequest)).to(Monitor.sink(monitorRef)).run()
    testProbe.expectNoMessage(timeout)

    // Connect a server and check for heartbeats again
    testProbe.send(monitorRef, Monitor.AtLeastOneServerConnected)
    testProbe.expectMsgType[DbActor.GenerateHeartbeat](timeout)
    testProbe.reply(DbActor.DbActorGenerateHeartbeatAck(HashMap()))

    monitorRef ! PoisonPill
    testProbe.expectTerminated(monitorRef)
  }

  test("monitor should send a ConnectTo() upon creation") {

    val mockConnectionMediator = TestProbe()

    // Write to mock server peers config file
    val mockConfig = List("mockConfig")
    testWriteToServerPeersConfig(mockConfig)

    val monitorRef = system.actorOf(Monitor.props(ActorRef.noSender))

    mockConnectionMediator.watch(monitorRef)

    // Ping monitor to inform it of ConnectionMediatorRef
    mockConnectionMediator.send(monitorRef, ConnectionMediator.Ping())

    // Expect first read of the server peers list
    mockConnectionMediator.expectMsgType[ConnectionMediator.ConnectTo](timeout)

    monitorRef ! PoisonPill
    mockConnectionMediator.expectTerminated(monitorRef)
  }

  test("monitor should send ConnectTo() requests to ConnectionMediator upon relevant config file change besides first read") {

    val mockConnectionMediator = TestProbe()

    // Write to mock server peers config file
    val mockConfig = List("mockConfig")
    testWriteToServerPeersConfig(mockConfig)

    val monitorRef = system.actorOf(Monitor.props(ActorRef.noSender))

    mockConnectionMediator.watch(monitorRef)

    // Ping monitor to inform it of ConnectionMediatorRef
    mockConnectionMediator.send(monitorRef, ConnectionMediator.Ping())

    // Expect first read of the server peers list
    mockConnectionMediator.expectMsgType[ConnectionMediator.ConnectTo](timeout)

    // Expect no message as long as the server peers list is untouched
    mockConnectionMediator.expectNoMessage(timeout)

    val newContent = List("some", "strings")
    testWriteToServerPeersConfig(newContent)

    mockConnectionMediator.expectMsgType[ConnectionMediator.ConnectTo](timeout)

    monitorRef ! PoisonPill
    mockConnectionMediator.expectTerminated(monitorRef)
  }

  test("monitor should not react upon non relevant events in config directory besides first read") {

    val mockConnectionMediator = TestProbe()
    val monitorRef = system.actorOf(Monitor.props(ActorRef.noSender))

    mockConnectionMediator.watch(monitorRef)

    // Write to mock server peers config file
    val mockConfig = List("mockConfig")
    testWriteToServerPeersConfig(mockConfig)

    // Ping monitor to inform it of ConnectionMediatorRef
    mockConnectionMediator.send(monitorRef, ConnectionMediator.Ping())

    // Expect first read of the server peers list
    mockConnectionMediator.expectMsgType[ConnectionMediator.ConnectTo](timeout)

    // Create new file in the directory
    val filePath = Path.of(serverPeersListPath).getParent.toString + File.separator + "DELETE_ME"
    val file = new PrintWriter(filePath)
    file.write("Hello")
    file.close()

    // Set the file we created to delete itself after the jvm shutdown
    new File(filePath).deleteOnExit()

    mockConnectionMediator.expectNoMessage(timeout)

    monitorRef ! PoisonPill
    mockConnectionMediator.expectTerminated(monitorRef)
  }

  test("monitor should send a result to the connectionMediator") {

    val mockConnectionMediator = TestProbe()
    val monitorRef = system.actorOf(
      Monitor.props(toyDbActorRef, heartbeatRate = fastRate, messageDelay = fastRate)
    )
    mockConnectionMediator.watch(monitorRef)

    val expected = Map(CHANNEL1 -> Set(MESSAGE1_ID), CHANNEL2 -> Set(MESSAGE4_ID))

    // Ping monitor to inform it of ConnectionMediatorRef
    mockConnectionMediator.send(monitorRef, ConnectionMediator.Ping())
    mockConnectionMediator.expectMsgType[ConnectionMediator.ConnectTo](timeout)

    mockConnectionMediator.send(monitorRef, Monitor.TriggerHeartbeat)
    mockConnectionMediator.expectMsg(timeout, Heartbeat(expected))

    monitorRef ! PoisonPill
    mockConnectionMediator.expectTerminated(monitorRef)
  }

  test("monitor should send nothing when failing to query the data base") {

    val mockConnectionMediator = TestProbe()
    val monitorRef = system.actorOf(
      Monitor.props(failingToyDbActorRef, heartbeatRate = fastRate, messageDelay = fastRate)
    )

    mockConnectionMediator.watch(monitorRef)

    mockConnectionMediator.send(monitorRef, ConnectionMediator.Ping())
    mockConnectionMediator.expectMsgType[ConnectionMediator.ConnectTo](timeout)

    mockConnectionMediator.send(monitorRef, Monitor.TriggerHeartbeat)
    mockConnectionMediator.expectNoMessage(timeout)

    monitorRef ! PoisonPill
    mockConnectionMediator.expectTerminated(monitorRef)
  }

}
