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
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.{GreetServer, Rumor}
import ch.epfl.pop.model.objects.{Base64Data, PublicKey}
import ch.epfl.pop.pubsub.ClientActor.ClientAnswer
import ch.epfl.pop.pubsub.graph.GraphMessage
import ch.epfl.pop.storage.DbActor.{DbActorReadRumors, ReadRumors}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers.{a, shouldBe}

import scala.concurrent.Await

class RumorHandlerSuite extends TestKit(ActorSystem("RumorActorSuiteActorSystem")) with AnyFunSuiteLike with AskPatternConstants with BeforeAndAfterAll {

  val inMemoryStorage: InMemoryStorage = InMemoryStorage()
  val messageRegistry: MessageRegistry = MessageRegistry()
  val pubSubMediatorRef: ActorRef = system.actorOf(PubSubMediator.props, "PubSubMediator")
  val dbActorRef: AskableActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, messageRegistry, inMemoryStorage)), "DbActor")
  val securityModuleActorRef: AskableActorRef = system.actorOf(Props(SecurityModuleActor(RuntimeEnvironment.securityPath)))
  val monitorRef: ActorRef = system.actorOf(Monitor.props(dbActorRef))
  val connectionMediatorRef: AskableActorRef = system.actorOf(ConnectionMediator.props(monitorRef, pubSubMediatorRef, dbActorRef, securityModuleActorRef, messageRegistry))

  val rumorHandler: Flow[GraphMessage, GraphMessage, NotUsed] = ParamsHandler.rumorHandler(dbActorRef, connectionMediatorRef)

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

    val readRumor = dbActorRef ? ReadRumors(Map(rumor.senderPk -> List(rumor.rumorId)))
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

}
