package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.Flow
import akka.testkit.TestKit
import ch.epfl.pop.IOHelper
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.pubsub.graph.GraphMessage
import ch.epfl.pop.pubsub.{MessageRegistry, PubSubMediator}
import ch.epfl.pop.storage.{DbActor, InMemoryStorage}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.funsuite.AnyFunSuiteLike
import akka.pattern.ask

class RumorStateAnsHandlerSuite extends TestKit(ActorSystem("RumorStateAnsHandler")) with AnyFunSuiteLike with BeforeAndAfterAll with BeforeAndAfterEach {
  private var inMemoryStorage: InMemoryStorage = _
  private var messageRegistry: MessageRegistry = _
  private var pubSubMediatorRef: ActorRef = _
  private var dbActorRef: AskableActorRef = _
  private var rumorStateHandler: Flow[GraphMessage, GraphMessage, NotUsed] = _
  private val rumorState = JsonRpcRequest.buildFromJson(IOHelper.readJsonFromPath("src/test/scala/util/examples/json/rumor_state/rumor_state.json"))

  override def beforeAll(): Unit = {
    inMemoryStorage = InMemoryStorage()
    messageRegistry = MessageRegistry()
    pubSubMediatorRef = system.actorOf(PubSubMediator.props, "pubSubRumorState")
    dbActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, messageRegistry, inMemoryStorage)), "dbRumorStateAns")
    rumorStateHandler = ParamsHandler.rumorStateHandler(dbActorRef)
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
