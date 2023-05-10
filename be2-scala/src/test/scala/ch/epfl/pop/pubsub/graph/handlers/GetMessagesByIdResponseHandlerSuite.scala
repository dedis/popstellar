package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.testkit.TestKit
import akka.util.Timeout
import ch.epfl.pop.model.network.JsonRpcResponse
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.Channel
import ch.epfl.pop.pubsub.graph.GraphMessage
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PubSubMediator, PublishSubscribe}
import ch.epfl.pop.storage.{DbActor, InMemoryStorage}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, equal}

import scala.concurrent.Await
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.io.BufferedSource
import scala.io.Source.fromFile
import scala.util.Success

class GetMessagesByIdResponseHandlerSuite extends TestKit(ActorSystem("GetMessagesByIdResponseHandlerSuiteSystem")) with AnyFunSuiteLike with AskPatternConstants with BeforeAndAfterAll {

  // Implicit for system actors
  implicit val timeout: Timeout = Timeout(1.seconds)

  val MAX_TIME: FiniteDuration = 2 * duration
  val inMemoryStorage: InMemoryStorage = InMemoryStorage()
  val messageRegistry: MessageRegistry = MessageRegistry()
  val pubSubMediatorRef: ActorRef = system.actorOf(PubSubMediator.props, "PubSubMediator")
  val dbActorRef: AskableActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, messageRegistry, inMemoryStorage)), "DbActor")

  // Inject dbActor above
  PublishSubscribe.buildGraph(pubSubMediatorRef, dbActorRef, messageRegistry, ActorRef.noSender, ActorRef.noSender, isServer = false)

  // handler we want to test
  val responseHandler: Flow[GraphMessage, GraphMessage, NotUsed] = GetMessagesByIdResponseHandler.responseHandler(MessageRegistry())

  // loading the valid json
  val sourceCorrectGetMessageById: BufferedSource =
    fromFile("src/test/scala/util/examples/json/get_messages_by_id_answer.json")

  val linesCorrectJson: String =
    try {
      sourceCorrectGetMessageById.mkString
    } finally {
      sourceCorrectGetMessageById.close()
    }

  // loading the invalid json
  val sourceIncorrectGetMessageById: BufferedSource =
    fromFile("src/test/scala/util/examples/json/get_messages_by_id_answer_with_wrong_messages.json")

  val linesIncorrectJson: String =
    try {
      sourceIncorrectGetMessageById.mkString
    } finally {
      sourceIncorrectGetMessageById.close()
    }

  val incorrectGetMessagesByIdResponse: JsonRpcResponse = JsonRpcResponse.buildFromJson(linesIncorrectJson)
  val getMessagesByIdResponse: JsonRpcResponse = JsonRpcResponse.buildFromJson(linesCorrectJson)

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
