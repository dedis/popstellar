package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.actor.Status.Failure
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.testkit.{TestKit, TestProbe}
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PubSubMediator, PublishSubscribe}
import ch.epfl.pop.storage.{DbActor, InMemoryStorage, SecurityModuleActor}
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, Sink, Source}
import ch.epfl.pop.IOHelper.readJsonFromPath
import ch.epfl.pop.config.RuntimeEnvironment
import ch.epfl.pop.decentralized.{ConnectionMediator, Monitor}
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.model.network.method.{GreetServer, Rumor}
import ch.epfl.pop.model.objects.{Base64Data, Channel, PublicKey}
import ch.epfl.pop.pubsub.ClientActor.ClientAnswer
import ch.epfl.pop.pubsub.graph.GraphMessage
import ch.epfl.pop.storage.DbActor.{DbActorReadRumor, ReadRumor}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.{a, equal, should, shouldBe}

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration

class RumorHandlerSuite extends TestKit(ActorSystem("RumorActorSuiteActorSystem")) with AnyFunSuiteLike with AskPatternConstants with BeforeAndAfterAll with BeforeAndAfterEach {

  val MAX_TIME: FiniteDuration = duration.mul(2)

  private val inMemoryStorage: InMemoryStorage = InMemoryStorage()
  private val messageRegistry: MessageRegistry = MessageRegistry()
  private val pubSubMediatorRef: ActorRef = system.actorOf(PubSubMediator.props)
  private val dbActorRef: AskableActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, messageRegistry, inMemoryStorage)))
  private val securityModuleActorRef: AskableActorRef = system.actorOf(Props(SecurityModuleActor(RuntimeEnvironment.securityPath)))
  private val monitorRef: ActorRef = system.actorOf(Monitor.props(dbActorRef))
  private var connectionMediatorRef: AskableActorRef = system.actorOf(ConnectionMediator.props(monitorRef, pubSubMediatorRef, dbActorRef, securityModuleActorRef, messageRegistry))
  private val rumorHandler: Flow[GraphMessage, GraphMessage, NotUsed] = ParamsHandler.rumorHandler(dbActorRef, messageRegistry)
  // Inject dbActor above
  PublishSubscribe.buildGraph(pubSubMediatorRef, dbActorRef, securityModuleActorRef, messageRegistry, ActorRef.noSender, ActorRef.noSender, ActorRef.noSender, isServer = false)

  val pathCorrectRumor: String = "src/test/scala/util/examples/json/rumor/rumor_correct_msg.json"

  val rumorRequest: JsonRpcRequest = JsonRpcRequest.buildFromJson(readJsonFromPath(pathCorrectRumor))

  val rumor: Rumor = rumorRequest.getParams.asInstanceOf[Rumor]

  private val nbMessages = rumor.messages.values.foldLeft(0)((acc, msgs) => acc + msgs.size)
  private var processDuration: FiniteDuration = duration.mul(nbMessages)

  override def beforeEach(): Unit = {
    inMemoryStorage.elements = Map.empty
  }

  // Helper function
  // Get rid of decoded data to compare against original message
  private def getMessages(channel: Channel): Set[Message] = {
    val ask = dbActorRef ? DbActor.Catchup(channel)
    Await.result(ask, MAX_TIME) match {
      case DbActor.DbActorCatchupAck(list) => list
          .map(msg => Message(msg.data, msg.sender, msg.signature, msg.message_id, msg.witness_signatures, None)).toSet
      case _ => Matchers.fail(s"Couldn't catchup on channel: $channel")
    }
  }

  override def afterAll(): Unit = {
    // Stops the testKit
    TestKit.shutdownActorSystem(system)
  }

  test("rumor handler should write new rumors in memory") {
    val output = Source.single(Right(rumorRequest)).via(rumorHandler).runWith(Sink.head)

    Await.result(output, processDuration)

    val readRumor = dbActorRef ? ReadRumor(rumor.senderPk -> rumor.rumorId)
    Await.result(readRumor, duration) shouldBe DbActorReadRumor(Some(rumor))
  }

  test("rumor handler should handle correct rumors without error") {
    val output = Source.single(Right(rumorRequest)).via(rumorHandler).runWith(Sink.head)

    Await.result(output, duration) shouldBe a[Right[_, _]]
  }

  test("rumor handler should fail on processing something else than a rumor") {
    val publishRequest: JsonRpcRequest = JsonRpcRequest.buildFromJson(readJsonFromPath("src/test/scala/util/examples/json/election/open_election.json"))

    val output = Source.single(Right(publishRequest)).via(rumorHandler).runWith(Sink.head)

    Await.result(output, processDuration) shouldBe a[Left[_, _]]

  }

  test("rumor handler should output a success response if rumor is a new rumor") {

    val output = Source.single(Right(rumorRequest)).via(rumorHandler).runWith(Sink.head)

    val outputResult = Await.result(output, processDuration)

    outputResult shouldBe a[Right[_, JsonRpcResponse]]

    outputResult match
      case Right(jsonRpcResponse: JsonRpcResponse) => jsonRpcResponse.result.isDefined shouldBe true
      case _                                       => 1 shouldBe 0

  }

  test("rumor handler should output a error response if rumor is a old rumor") {
    val dbWrite = dbActorRef ? DbActor.WriteRumor(rumor)
    Await.result(dbWrite, processDuration)

    val output = Source.single(Right(rumorRequest)).via(rumorHandler).runWith(Sink.head)

    val outputResult = Await.result(output, duration)

    outputResult shouldBe a[Right[_, JsonRpcResponse]]

    outputResult match
      case Right(jsonRpcResponse: JsonRpcResponse) => jsonRpcResponse.error.isDefined shouldBe true
      case _                                       => 1 shouldBe 0
  }

  test("rumor handler should process messages received in a rumor") {
    val output = Source.single(Right(rumorRequest)).via(rumorHandler).runWith(Sink.head)

    val outputResult = Await.result(output, processDuration)

    val ask = dbActorRef ? DbActor.GetAllChannels()
    val channelsInDb = Await.result(ask, MAX_TIME) match {
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
