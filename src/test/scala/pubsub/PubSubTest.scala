package pubsub

import java.util.concurrent.TimeUnit

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Props, SpawnProtocol}
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Sink, Source}
import akka.util.Timeout
import ch.epfl.pop
import ch.epfl.pop.json.JsonMessages._
import ch.epfl.pop.pubsub.{ChannelActor, PublishSubscribe}
import org.scalatest.FunSuite

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class PubSubTest extends FunSuite {

  private def sendAndVerify(messages: List[(JsonMessage, Option[JsonMessage])]): Unit = {
    sendAndVerify(messages.map { case (m, o) => (Some(m), o, 0) }, 1)
  }

  private def sendAndVerify(messages: List[(Option[JsonMessage], Option[JsonMessage], Int)], numberProcesses: Int): Unit = {
    val root = Behaviors.setup[SpawnProtocol.Command] { context =>
      SpawnProtocol()
    }
    implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem[SpawnProtocol.Command](root, "test")
    implicit val timeout = Timeout(1, TimeUnit.SECONDS)
    val (entry, actor) = setup()
    val sinkHead = Sink.head[JsonMessage]

    val flows = (1 to numberProcesses).map(_ => PublishSubscribe.getFlow(entry, actor))
    messages.foreach { case (message, response, flowNumber) =>
      val source = message match {
        case Some(m) => Source.single(m)
        case None => Source.empty
      }
      val future = source.initialDelay(1.milli).via(flows(flowNumber)).runWith(sinkHead)
      response match {
        case Some(json) => assert(Await.result(future, 1.seconds) == json)
        case None =>
      }
    }

  }

  private def setup()(implicit system: ActorSystem[SpawnProtocol.Command], timeout: Timeout) = {
    implicit val ec = system.executionContext

    val (entry, exit) = MergeHub.source[PublishChannelClient].toMat(BroadcastHub.sink)(Keep.both).run()
    val futureActor: Future[ActorRef[ChannelActor.ChannelMessage]] = system.ask(SpawnProtocol.Spawn(pop.pubsub.ChannelActor(exit),
      "actor", Props.empty, _))
    val actor = Await.result(futureActor, 1.seconds)
    (entry, actor)
  }

  private val AnswerOk: Some[AnswerMessageServer] = Some(AnswerMessageServer(true, None))

  implicit def message2Option(jsonMessage: JsonMessage) = Some(jsonMessage)

  test("Create a channel, subscribe to it and publish a message") {

    val l = List((CreateChannelClient("main", "none"), AnswerOk),
      (SubscribeChannelClient("main"), AnswerOk),
      (PublishChannelClient("main", "Hello on main channel"), Some(PublishChannelClient("main", "Hello on main channel")))
    )

    sendAndVerify(l)

  }

  test("Create a channel, subscribe to it and publish multiple messages") {

    val l = List((CreateChannelClient("main", "none"), AnswerOk),
      (SubscribeChannelClient("main"), AnswerOk),
      (PublishChannelClient("main", "Hello on main channel"), Some(PublishChannelClient("main", "Hello on main channel"))),
      (PublishChannelClient("main", "Still here"), Some(PublishChannelClient("main", "Still here"))),
      (PublishChannelClient("main", "Is there anyone?"), Some(PublishChannelClient("main", "Is there anyone?")))
    )

    sendAndVerify(l)

  }

  test("Create multiple channels, subscribe to them and publish multiple messages") {

    val l = List((CreateChannelClient("main", "none"), AnswerOk),
      (SubscribeChannelClient("main"), AnswerOk),
      (CreateChannelClient("chan2", "none"), AnswerOk),
      (SubscribeChannelClient("chan2"), AnswerOk),
      (PublishChannelClient("main", "Hello on main channel"), Some(PublishChannelClient("main", "Hello on main channel"))),
      (PublishChannelClient("main", "Still here"), Some(PublishChannelClient("main", "Still here"))),
      (PublishChannelClient("chan2", "Hello chan2"), Some(PublishChannelClient("chan2", "Hello chan2"))),
      (PublishChannelClient("main", "Is there anyone?"), Some(PublishChannelClient("main", "Is there anyone?"))),
      (PublishChannelClient("chan2", "bye"), Some(PublishChannelClient("chan2", "bye"))),
    )

    sendAndVerify(l)

  }

  test("Two process subscribe and publish on the same channel") {
    val l: List[(Option[JsonMessage], Option[JsonMessage], Int)] =
      List((CreateChannelClient("main", "none"), AnswerOk, 0),
        (SubscribeChannelClient("main"), AnswerOk, 0),
        (SubscribeChannelClient("main"), AnswerOk, 1),
        (PublishChannelClient("main", "Hello main!"), PublishChannelClient("main", "Hello main!"), 0),
        (PublishChannelClient("main", "message2"), PublishChannelClient("main", "message2"), 0),
        (PublishChannelClient("main", "Hello main!"), PublishChannelClient("main", "Hello main!"), 1),

      )
    sendAndVerify(l, 2)
  }

  test("Error when subscribing to a channel that does not exist") {
    val l = List((SubscribeChannelClient("unknown"), Some(AnswerMessageServer(false, Some("Unknown channel.")))))
    sendAndVerify(l)
  }

  test("Error when creating an existing channel") {
    val l = List((CreateChannelClient("main", "empty"), AnswerOk),
      (CreateChannelClient("main", "empty"), Some(AnswerMessageServer(false, Some("The channel already exist")))))
    sendAndVerify(l)
  }


}
