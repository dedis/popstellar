package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.testkit.TestKit
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PubSubMediator}
import ch.epfl.pop.storage.{DbActor, InMemoryStorage}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuiteLike
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import ch.epfl.pop.pubsub.graph.GraphMessage

class RumorStateHandlerSuite extends TestKit(ActorSystem("RumorStateSuiteActorSystem")) with AnyFunSuiteLike with AskPatternConstants with BeforeAndAfterAll {

  private var inMemoryStorage: InMemoryStorage = _
  private var messageRegistry: MessageRegistry = _
  private var pubSubMediatorRef: ActorRef = _
  private var dbActorRef: AskableActorRef = _
  private var rumorStateHAndler: Flow[GraphMessage, GraphMessage, NotUsed] = _

  override def beforeAll(): Unit = {
    inMemoryStorage = InMemoryStorage()
    messageRegistry = MessageRegistry()
    pubSubMediatorRef = system.actorOf(PubSubMediator.props, "pubSubRumorState")
    dbActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, messageRegistry, inMemoryStorage)), "dbRumorState")
    rumorStateHAndler = ParamsHandler.rumorStateHandler(dbActorRef)
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

}
