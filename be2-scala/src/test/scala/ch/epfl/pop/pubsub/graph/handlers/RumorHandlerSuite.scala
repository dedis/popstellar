package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.actor.Status.Failure
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.testkit.{TestKit, TestProbe}
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PubSubMediator}
import ch.epfl.pop.storage.{DbActor, InMemoryStorage, SecurityModuleActor}
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, Sink, Source}
import ch.epfl.pop.IOHelper.readJsonFromPath
import ch.epfl.pop.config.RuntimeEnvironment
import ch.epfl.pop.decentralized.{ConnectionMediator, Monitor}
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.model.network.method.{GreetServer, Rumor}
import ch.epfl.pop.model.objects.{Base64Data, PublicKey}
import ch.epfl.pop.pubsub.ClientActor.ClientAnswer
import ch.epfl.pop.pubsub.graph.GraphMessage
import ch.epfl.pop.storage.DbActor.{DbActorReadRumors, ReadRumors}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers.{a, shouldBe}

import scala.concurrent.Await

class RumorHandlerSuite extends TestKit(ActorSystem("RumorActorSuiteActorSystem")) with AnyFunSuiteLike with AskPatternConstants with BeforeAndAfterAll with BeforeAndAfterEach {

  private var inMemoryStorage: InMemoryStorage = _
  private var messageRegistry: MessageRegistry = _
  private var pubSubMediatorRef: ActorRef = _
  private var dbActorRef: AskableActorRef = _
  private var securityModuleActorRef: AskableActorRef = _
  private var monitorRef: ActorRef = _
  private var connectionMediatorRef: AskableActorRef = _
  private var rumorHandler: Flow[GraphMessage, GraphMessage, NotUsed] = _

  override def beforeEach(): Unit = {
    inMemoryStorage = InMemoryStorage()
    messageRegistry = MessageRegistry()
    pubSubMediatorRef = system.actorOf(PubSubMediator.props)
    dbActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, messageRegistry, inMemoryStorage)))
    securityModuleActorRef = system.actorOf(Props(SecurityModuleActor(RuntimeEnvironment.securityPath)))
    monitorRef = system.actorOf(Monitor.props(dbActorRef))
    connectionMediatorRef = system.actorOf(ConnectionMediator.props(monitorRef, pubSubMediatorRef, dbActorRef, securityModuleActorRef, messageRegistry))

    rumorHandler = ParamsHandler.rumorHandler(dbActorRef, connectionMediatorRef)
  }

  val pathCorrectRumor: String = "src/test/scala/util/examples/json/rumor/rumor.json"

  val rumorRequest: JsonRpcRequest = JsonRpcRequest.buildFromJson(readJsonFromPath(pathCorrectRumor))

  val rumor: Rumor = rumorRequest.getParams.asInstanceOf[Rumor]

  override def afterAll(): Unit = {
    // Stops the testKit
    TestKit.shutdownActorSystem(system)
  }

  test("rumor handler should write new rumors in memory") {
    val output = Source.single(Right(rumorRequest)).via(rumorHandler).runWith(Sink.head)

    Await.result(output, duration)

    val readRumor = dbActorRef ? ReadRumors(rumor.senderPk -> rumor.rumorId)
    Await.result(readRumor, duration) shouldBe a[DbActorReadRumors]
  }

  test("rumor handler should handle correct rumors without error") {
    val output = Source.single(Right(rumorRequest)).via(rumorHandler).runWith(Sink.head)

    Await.result(output, duration) shouldBe a[Right[_, _]]
  }

  test("rumor handler should fail on processing something else than a rumor") {
    val publishRequest: JsonRpcRequest = JsonRpcRequest.buildFromJson(readJsonFromPath("src/test/scala/util/examples/json/election/open_election.json"))

    val output = Source.single(Right(publishRequest)).via(rumorHandler).runWith(Sink.head)

    Await.result(output, duration) shouldBe a[Left[_, _]]

  }

  test("rumor handler should output a success response if rumor is a new rumor") {

    val output = Source.single(Right(rumorRequest)).via(rumorHandler).runWith(Sink.head)

    val outputResult = Await.result(output, duration)

    outputResult shouldBe a[Right[_, JsonRpcResponse]]

    outputResult match
      case Right(jsonRpcResponse: JsonRpcResponse) => jsonRpcResponse.result.isDefined shouldBe true
      case _                                       => 1 shouldBe 0

  }

  test("rumor handler should output a error response if rumor is a old rumor") {
    val dbWrite = dbActorRef ? DbActor.WriteRumor(rumor)
    Await.result(dbWrite, duration)

    val output = Source.single(Right(rumorRequest)).via(rumorHandler).runWith(Sink.head)

    val outputResult = Await.result(output, duration)

    outputResult shouldBe a[Right[_, JsonRpcResponse]]

    outputResult match
      case Right(jsonRpcResponse: JsonRpcResponse) => jsonRpcResponse.error.isDefined shouldBe true
      case _                                       => 1 shouldBe 0
  }
}
