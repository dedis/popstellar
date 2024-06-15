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
import akka.stream.scaladsl.{Flow, Sink, Source}
import ch.epfl.pop.IOHelper
import ch.epfl.pop.model.network.MethodType.rumor_state
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse, ResultEmptyList, ResultObject, ResultRumor}
import ch.epfl.pop.model.network.method.{Rumor, RumorState}
import ch.epfl.pop.model.objects.{Base64Data, PublicKey}
import ch.epfl.pop.pubsub.graph.GraphMessage
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.storage.DbActor.DbActorAck
import org.scalatest.matchers.should.Matchers.{a, shouldBe}
import util.examples.Rumor.RumorExample

import scala.concurrent.Await

class RumorStateHandlerSuite extends TestKit(ActorSystem("RumorStateSuiteActorSystem")) with AnyFunSuiteLike with AskPatternConstants with BeforeAndAfterAll {

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
    dbActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, messageRegistry, inMemoryStorage)), "dbRumorState")
    rumorStateHandler = ParamsHandler.rumorStateHandler(dbActorRef)
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  test("rumor state handler generate right type of response") {
    val output = Source.single(Right(rumorState)).via(rumorStateHandler).runWith(Sink.head)

    Await.result(output, duration) shouldBe a[Right[_, _]]
  }

  test("rumor state handler fails on wrong input") {
    val rumorRpc = JsonRpcRequest.buildFromJson(IOHelper.readJsonFromPath("src/test/scala/util/examples/json/rumor/rumor.json"))
    val outputRumor = Source.single(Right(rumorRpc)).via(rumorStateHandler).runWith(Sink.head)

    Await.result(outputRumor, duration) shouldBe a[Left[_, _]]

    val responseRpc = JsonRpcResponse(RpcValidator.JSON_RPC_VERSION, ResultObject(0), None)
    val outputResponse = Source.single(Right(responseRpc)).via(rumorStateHandler).runWith(Sink.head)

    Await.result(outputResponse, duration) shouldBe a[Left[_, _]]
  }

  test("rumor state handler should return the right list of rumors") {
    val publicKey = PublicKey(Base64Data.encode("publicKey"))
    val rumorList: List[Rumor] = (0 to 10).map(i => Rumor(publicKey, i, Map.empty)).toList

    for (rumor <- rumorList)
      val writeResult = dbActorRef ? DbActor.WriteRumor(rumor)
      Await.result(writeResult, duration) shouldBe a[DbActorAck]

    val rumorState = RumorState(Map(
      publicKey -> 5
    ))

    val rumorStateRpc = JsonRpcRequest(RpcValidator.JSON_RPC_VERSION, rumor_state, rumorState, Some(0))
    val output = Source.single(Right(rumorStateRpc)).via(rumorStateHandler).runWith(Sink.head)
    val rumorListResult = rumorList.filter(_.rumorId > 5)
    val resultObject = ResultObject(ResultRumor(rumorListResult))
    Await.result(output, duration) shouldBe Right(JsonRpcResponse(RpcValidator.JSON_RPC_VERSION, resultObject, rumorStateRpc.id))

  }

}
