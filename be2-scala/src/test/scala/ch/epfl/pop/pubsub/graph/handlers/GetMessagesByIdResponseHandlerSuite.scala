package ch.epfl.pop.pubsub.graph.handlers

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import akka.util.Timeout
import ch.epfl.pop.model.network.JsonRpcResponse
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.Channel
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PubSubMediator, PublishSubscribe}
import ch.epfl.pop.storage.{DbActor, InMemoryStorage}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, equal}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.io.BufferedSource
import scala.util.Success

class GetMessagesByIdResponseHandlerSuite extends TestKit(ActorSystem("GetMessagesByIdResponseHandlerSuiteSystem")) with AnyFunSuiteLike with AskPatternConstants with BeforeAndAfterAll {

  // Implicit for system actors
  implicit val timeout: Timeout = Timeout(1.seconds)

  val inMemoryStorage: InMemoryStorage = InMemoryStorage()
  val messageRegistry: MessageRegistry = MessageRegistry()
  val pubSubMediatorRef: ActorRef = system.actorOf(PubSubMediator.props, "PubSubMediator")
  val dbActorRef: AskableActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, messageRegistry, inMemoryStorage)), "DbActor")

  // Inject dbActor above
  PublishSubscribe.buildGraph(pubSubMediatorRef, dbActorRef, messageRegistry, ActorRef.noSender, ActorRef.noSender, isServer = false)

  // Helper
  // Get rid of decoded data to compare against original message
  private def getMessages(channel: Channel): Set[Message] = {
    val ask = dbActorRef ? DbActor.Catchup(channel)
    Await.result(ask, duration) match {
      case DbActor.DbActorCatchupAck(list) => list
          .map(msg => Message(msg.data, msg.sender, msg.signature, msg.message_id, msg.witness_signatures, None)).toSet
      case _ => Matchers.fail(s"Couldn't catchup on channel: $channel")
    }
  }

  override def afterAll(): Unit = {
    // Stops the testKit
    TestKit.shutdownActorSystem(system)
  }

  val input_file = "src/test/scala/util/examples/json/get_messages_by_id_answer.json"
  val source: BufferedSource = scala.io.Source.fromFile(input_file)
  val lines: String =
    try source.mkString
    finally source.close()
  val getMessagesByIdResponse: JsonRpcResponse = JsonRpcResponse.buildFromJson(lines)

  test("get_messages_by_id response should be processed without errors") {
    val handler = GetMessagesByIdResponseHandler.graph(pubSubMediatorRef, MessageRegistry())
    val output = Source.single(Right(getMessagesByIdResponse)).via(handler).runWith(Sink.head)

    Await.ready(output, duration).value.get match {
      case Success(Right(_)) => 0 should equal(0)
      case err @ _           => Matchers.fail(err.toString)
    }
  }

  test("After processing a correct get_messages_by_id response the db should contain all the channels and messages from it") {

    val ask = dbActorRef ? DbActor.GetAllChannels()
    val channelsInDb = Await.result(ask, duration) match {
      case DbActor.DbActorGetAllChannelsAck(channels) => channels
      case err @ _                                    => Matchers.fail("Error: " + err.toString)
    }

    val channelsInResponse = getMessagesByIdResponse.result.get.resultMap.get.keySet
    channelsInResponse.diff(channelsInDb) should equal(Set.empty)

    val messagesInResponse = getMessagesByIdResponse.result.get.resultMap.get.values.foldLeft(Set.empty: Set[Message])((acc, set) => acc ++ set)
    val allMessages: Set[Message] = channelsInDb.foldLeft(Set.empty: Set[Message])((acc, channel) => acc ++ getMessages(channel))

    messagesInResponse.diff(allMessages) should equal(Set.empty)
  }
}
