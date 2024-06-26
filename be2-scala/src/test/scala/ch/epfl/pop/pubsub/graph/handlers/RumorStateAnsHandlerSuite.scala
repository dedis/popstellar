package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.testkit.TestKit
import ch.epfl.pop.IOHelper
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse, ResultObject, ResultRumor}
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PubSubMediator, PublishSubscribe}
import ch.epfl.pop.storage.{DbActor, InMemoryStorage}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.funsuite.AnyFunSuiteLike
import akka.pattern.ask
import ch.epfl.pop.model.network.MethodType.publish
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.{Publish, Rumor, RumorState}
import ch.epfl.pop.model.objects.{Channel, RumorData}
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.storage.DbActor.{DbActorReadRumorData, ReadRumorData}
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.{a, equal, should, shouldBe}
import util.examples.MessageExample

import scala.concurrent.Await

class RumorStateAnsHandlerSuite extends TestKit(ActorSystem("RumorStateAnsHandler")) with AnyFunSuiteLike with BeforeAndAfterAll with BeforeAndAfterEach with AskPatternConstants {
  private var inMemoryStorage: InMemoryStorage = _
  private var messageRegistry: MessageRegistry = _
  private var pubSubMediatorRef: ActorRef = _
  private var dbActorRef: AskableActorRef = _
  private var rumorStateAnsHandler: Flow[GraphMessage, GraphMessage, NotUsed] = _
  private val rumorStateResponse = JsonRpcResponse.buildFromJson(IOHelper.readJsonFromPath("src/test/scala/util/examples/json/rumor_state/rumor_state_ans.json"))

  override def beforeAll(): Unit = {
    inMemoryStorage = InMemoryStorage()
    messageRegistry = MessageRegistry()
    pubSubMediatorRef = system.actorOf(PubSubMediator.props, "pubSubRumorState")
    dbActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, messageRegistry, inMemoryStorage)), "dbRumorStateAns")
    rumorStateAnsHandler = ProcessMessagesHandler.rumorStateAnsHandler(dbActorRef, messageRegistry)

    PublishSubscribe.buildGraph(pubSubMediatorRef, dbActorRef, ActorRef.noSender, messageRegistry, ActorRef.noSender, ActorRef.noSender, ActorRef.noSender, false)
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  // Helper function
  // Get rid of decoded data to compare against original message
  private def getMessages(channel: Channel): Set[Message] = {
    val ask = dbActorRef ? DbActor.Catchup(channel)
    Await.result(ask, duration) match {
      case DbActor.DbActorCatchupAck(list) => list
          .map(msg => Message(msg.data, msg.sender, msg.signature, msg.message_id, msg.witness_signatures, None)).toSet
      case _ => Matchers.fail(s"Couldn't catchup on channel: $channel")
    }
  }

  test("rumor state ans processes all msg well") {

    val output = Source.single(Right(rumorStateResponse)).via(rumorStateAnsHandler).runWith(Sink.head)
    Await.result(output, duration)

    val ask = dbActorRef ? DbActor.GetAllChannels()
    val channelsInDb = Await.result(ask, duration) match {
      case DbActor.DbActorGetAllChannelsAck(channels) => channels
      case err @ _                                    => Matchers.fail(err.toString)
    }

    val rumorList = rumorStateResponse.result.get.resultRumor.get
    val channelsInRumor = rumorList.flatMap(rumor => rumor.messages.keySet).toSet
    channelsInRumor.diff(channelsInDb) should equal(Set.empty)

    val messagesInDb: Set[Message] = channelsInDb.foldLeft(Set.empty: Set[Message])((acc, channel) => acc ++ getMessages(channel))
    val messagesInRumor = rumorList.flatMap(rumor => rumor.messages.values).foldLeft(Set.empty: Set[Message])((acc, set) => acc ++ set)

    messagesInRumor.diff(messagesInDb) should equal(Set.empty)

    val readRumorData = dbActorRef ? ReadRumorData(rumorList.head.senderPk)
    Await.result(readRumorData, duration) match
      case DbActorReadRumorData(rumorData: RumorData) =>
        rumorData.rumorIds shouldBe rumorList.map(_.rumorId)
      case _ => 0 shouldBe 1
  }

  test("rumor state ans handler fails on wrong type") {
    val responseInt = JsonRpcResponse(RpcValidator.JSON_RPC_VERSION, ResultObject(0), Some(0))
    val outputResponseInt = Source.single(Right(responseInt)).via(rumorStateAnsHandler).runWith(Sink.head)
    Await.result(outputResponseInt, duration) shouldBe a[Left[PipelineError, Nothing]]

    val rumorList = rumorStateResponse.result.get.resultRumor.get
    val request = JsonRpcRequest(RpcValidator.JSON_RPC_VERSION, publish, Publish(rumorList.head.messages.head._1, rumorList.head.messages.head._2.head), None)
    val outputRequest = Source.single(Right(request)).via(rumorStateAnsHandler).runWith(Sink.head)
    Await.result(outputRequest, duration) shouldBe a[Left[PipelineError, Nothing]]

  }

}
