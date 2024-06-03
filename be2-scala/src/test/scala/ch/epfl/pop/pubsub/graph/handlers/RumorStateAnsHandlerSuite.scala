package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.testkit.TestKit
import ch.epfl.pop.IOHelper
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse, ResultObject, ResultRumor}
import ch.epfl.pop.pubsub.graph.GraphMessage
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PubSubMediator, PublishSubscribe}
import ch.epfl.pop.storage.{DbActor, InMemoryStorage}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.funsuite.AnyFunSuiteLike
import akka.pattern.ask
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.{Rumor, RumorState}
import ch.epfl.pop.model.objects.Channel
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.{equal, should}

import scala.concurrent.Await

class RumorStateAnsHandlerSuite extends TestKit(ActorSystem("RumorStateAnsHandler")) with AnyFunSuiteLike with BeforeAndAfterAll with BeforeAndAfterEach with AskPatternConstants {
  private var inMemoryStorage: InMemoryStorage = _
  private var messageRegistry: MessageRegistry = _
  private var pubSubMediatorRef: ActorRef = _
  private var dbActorRef: AskableActorRef = _
  private var rumorStateAnsHandler: Flow[GraphMessage, GraphMessage, NotUsed] = _
  private val rumorRequest = JsonRpcRequest.buildFromJson(IOHelper.readJsonFromPath("src/test/scala/util/examples/json/rumor/rumor_correct_msg.json"))
  private val rumor = rumorRequest.getParams.asInstanceOf[Rumor]

  override def beforeAll(): Unit = {
    inMemoryStorage = InMemoryStorage()
    messageRegistry = MessageRegistry()
    pubSubMediatorRef = system.actorOf(PubSubMediator.props, "pubSubRumorState")
    dbActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, messageRegistry, inMemoryStorage)), "dbRumorStateAns")
    rumorStateAnsHandler = ProcessMessagesHandler.rumorStateAnsHandler(messageRegistry)

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
    println(rumor)
    val splitMsg = rumor.messages.toList.flatMap((channel, msgList) => msgList.map(msg => (channel, List(msg))))
    var rumorId = -1
    val rumorList = splitMsg.map((channel, msgList) => {
      rumorId += 1
      Rumor(rumor.senderPk, rumorId, Map(channel -> msgList))
    }).toList
    println(rumorList)
    val rumorStateAnsMsg = Right(JsonRpcResponse(RpcValidator.JSON_RPC_VERSION, ResultObject(ResultRumor(rumorList)), Some(1)))

    val output = Source.single(rumorStateAnsMsg).via(rumorStateAnsHandler).runWith(Sink.head)
    Await.result(output, duration)

    val ask = dbActorRef ? DbActor.GetAllChannels()
    val channelsInDb = Await.result(ask, duration) match {
      case DbActor.DbActorGetAllChannelsAck(channels) => channels
      case err @ _                                    => Matchers.fail(err.toString)
    }

    val channelsInRumor = rumor.messages.keySet
    channelsInRumor.diff(channelsInDb) should equal(Set.empty)

    val messagesInDb: Set[Message] = channelsInDb.foldLeft(Set.empty: Set[Message])((acc, channel) => acc ++ getMessages(channel))
    val messagesInRumor = rumor.messages.values.foldLeft(Set.empty: Set[Message])((acc, set) => acc ++ set)

    messagesInRumor.diff(messagesInDb) should equal(Set.empty)
  }

}
