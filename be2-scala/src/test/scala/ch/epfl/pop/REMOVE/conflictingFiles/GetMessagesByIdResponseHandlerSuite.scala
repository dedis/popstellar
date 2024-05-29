package ch.epfl.pop.REMOVE.conflictingFiles

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.{AskableActorRef, ask}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.testkit.{TestKit, TestKitBase}
import akka.util.Timeout
import ch.epfl.pop.IOHelper.readJsonFromPath
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.model.network.method.Rumor
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.Channel
import ch.epfl.pop.pubsub.graph.GraphMessage
import ch.epfl.pop.pubsub.graph.handlers.{ParamsHandler, ProcessMessagesHandler}
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PubSubMediator, PublishSubscribe}
import ch.epfl.pop.storage.DbActor.{DbActorReadRumor, ReadRumor}
import ch.epfl.pop.storage.SecurityModuleActorSuite.testSecurityDirectory
import ch.epfl.pop.storage.{DbActor, InMemoryStorage, SecurityModuleActor}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.*

import scala.concurrent.Await
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Success

class GetMessagesByIdResponseHandlerSuite extends TestKitBase with AnyFunSuiteLike with AskPatternConstants with BeforeAndAfterAll with BeforeAndAfterEach{

  implicit val system: ActorSystem = ActorSystem("GetMessagesByIdResponseHandlerSuiteSystem")

  // Implicit for system actors
  implicit val timeout: Timeout = Timeout(1.seconds)

  private var inMemoryStorage: InMemoryStorage = _
  private var messageRegistry: MessageRegistry = _
  private var pubSubMediatorRef: ActorRef = _
  private var dbActorRef: AskableActorRef = _
  private var securityModuleActorRef: AskableActorRef = _
  private var rumorHandler: Flow[GraphMessage, GraphMessage, NotUsed] = _

  val MAX_TIME: FiniteDuration = duration.mul(2)


  override def beforeAll(): Unit = {
    println("beforeAll GetMessage")
    inMemoryStorage = InMemoryStorage()
    messageRegistry = MessageRegistry()
    pubSubMediatorRef = system.actorOf(PubSubMediator.props, "PubSubMediatorGetMsg")
    dbActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, messageRegistry, inMemoryStorage)), "DbActorGetMsg")
    securityModuleActorRef = system.actorOf(Props(SecurityModuleActor(testSecurityDirectory)))
    rumorHandler = ParamsHandler.rumorHandler(dbActorRef, messageRegistry)
    // Inject dbActor above
    PublishSubscribe.buildGraph(pubSubMediatorRef, dbActorRef, securityModuleActorRef, messageRegistry, ActorRef.noSender, ActorRef.noSender, ActorRef.noSender, isServer = false)

  }

  // handler we want to test
  val responseHandler: Flow[GraphMessage, GraphMessage, NotUsed] = ProcessMessagesHandler.getMsgByIdResponseHandler(MessageRegistry())

  // loading the files
  val pathIncorrectGetMessageById: String = "src/test/scala/util/examples/json/get_messages_by_id_answer_with_wrong_messages.json"
  val pathCorrectGetMessageById: String = "src/test/scala/util/examples/json/get_messages_by_id_answer.json"

  val incorrectGetMessagesByIdResponse: JsonRpcResponse =
    JsonRpcResponse.buildFromJson(
      readJsonFromPath(pathIncorrectGetMessageById)
    )

  val getMessagesByIdResponse: JsonRpcResponse = JsonRpcResponse.buildFromJson(
    readJsonFromPath(pathCorrectGetMessageById)
  )

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
    println("afterAll GetMsg")
    TestKit.shutdownActorSystem(system)
  }

  val pathCorrectRumor: String = "src/test/scala/util/examples/json/rumor/rumor_correct_msg.json"

  val rumorRequest: JsonRpcRequest = JsonRpcRequest.buildFromJson(readJsonFromPath(pathCorrectRumor))

  val rumor: Rumor = rumorRequest.getParams.asInstanceOf[Rumor]

  private val nbMessages = rumor.messages.values.foldLeft(0)((acc, msgs) => acc + msgs.size)
  private var processDuration: FiniteDuration = duration.mul(nbMessages)

  override def beforeEach(): Unit = {
    inMemoryStorage.elements = Map.empty
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

  // https://github.com/dedis/popstellar/issues/1870
  test("rumor handler should process messages received in a rumor") {
    val dbRef = PublishSubscribe.getDbActorRef
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



  test("get_messages_by_id response should be processed without errors") {
    val output = Source.single(Right(getMessagesByIdResponse)).via(responseHandler).runWith(Sink.head)

    Await.ready(output, MAX_TIME).value.get match {
      case Success(Right(_)) => 0 should equal(0)
      case err @ _           => Matchers.fail(err.toString)
    }
  }

  test("After processing a correct get_messages_by_id response the db should contain all the channels and messages from it") {
    val ask = dbActorRef ? DbActor.GetAllChannels()
    val channelsInDb = Await.result(ask, MAX_TIME) match {
      case DbActor.DbActorGetAllChannelsAck(channels) => channels
      case err @ _                                    => Matchers.fail("Error: " + err.toString)
    }

    val channelsInResponse = getMessagesByIdResponse.result.get.resultMap.get.keySet
    channelsInResponse.diff(channelsInDb) should equal(Set.empty)

    val messagesInDb: Set[Message] = channelsInDb.foldLeft(Set.empty: Set[Message])((acc, channel) => acc ++ getMessages(channel))
    val messagesInResponse = getMessagesByIdResponse.result.get.resultMap.get.values.foldLeft(Set.empty: Set[Message])((acc, set) => acc ++ set)

    messagesInResponse.diff(messagesInDb) should equal(Set.empty)
  }

  test("get_messages_by_id with invalid data should fail to process") {
    // remove all message from previous tests
    inMemoryStorage.elements = Map.empty

    val output = Source.single(Right(incorrectGetMessagesByIdResponse)).via(responseHandler).runWith(Sink.head)
    Await.ready(output, MAX_TIME).value.get match {
      case Success(Left(_)) => 0 should equal(0)
      case _                => Matchers.fail("Should fail")
    }
  }
}
