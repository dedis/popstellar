package pubsub

import java.io.File
import java.util.concurrent.TimeUnit

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Props, SpawnProtocol}
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Sink, Source}
import akka.util.Timeout
import ch.epfl.pop
import ch.epfl.pop.DBActor
import ch.epfl.pop.DBActor.DBMessage
import ch.epfl.pop.json.JsonMessages._
import ch.epfl.pop.pubsub.{ChannelActor, PublishSubscribe}
import org.iq80.leveldb.Options
import org.scalatest.FunSuite

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.reflect.io.Directory

class PubSubTest extends FunSuite {

  private def sendAndVerify(messages: List[(JsonMessage, Option[JsonMessage])]): Unit = {
    sendAndVerify(messages.map { case (m, o) => (Some(m), o, 0) }, 1)
  }

  private def sendAndVerify(messages: List[(Option[JsonMessage], Option[JsonMessage], Int)], numberProcesses: Int): Unit = {
    val root = Behaviors.setup[SpawnProtocol.Command] { context =>
      SpawnProtocol()
    }

    val DatabasePath: String = "database_test"
    val file = new File(DatabasePath)
    //Reset db from previous tests
    val directory = new Directory(file)
    directory.deleteRecursively()

    implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem[SpawnProtocol.Command](root, "test")
    implicit val timeout = Timeout(1, TimeUnit.SECONDS)
    val (entry, actor, dbActor) = setup(DatabasePath)
    val sinkHead = Sink.head[JsonMessage]

    val options: Options = new Options()
    options.createIfMissing(true)

    val flows = (1 to numberProcesses).map(_ => PublishSubscribe.jsonFlow(entry, actor, dbActor))
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

  private def setup(databasePath : String)(implicit system: ActorSystem[SpawnProtocol.Command], timeout: Timeout) = {
    implicit val ec = system.executionContext

    val (entry, exit) = MergeHub.source[NotifyChannelServer].toMat(BroadcastHub.sink)(Keep.both).run()
    val futureActor: Future[ActorRef[ChannelActor.ChannelMessage]] = system.ask(SpawnProtocol.Spawn(pop.pubsub.ChannelActor(exit),
      "actor", Props.empty, _))
    val actor = Await.result(futureActor, 1.seconds)
    val futureDBActor: Future[ActorRef[DBMessage]] = system.ask(SpawnProtocol.Spawn(DBActor(databasePath), "actorDB", Props.empty, _))
    val dbActor = Await.result(futureDBActor, 1.seconds)

    (entry, actor, dbActor)
  }

  private val AnswerOk: Some[AnswerMessageServer] = Some(AnswerMessageServer(true, None))

  implicit def message2Option(jsonMessage: JsonMessage) = Some(jsonMessage)

  test("Create a channel, subscribe to it and publish a message") {

    val channel = "main"
    val m1 = "Hello on main channel"
    val id1 = m1
    val l = List((CreateChannelClient(channel, "none"), AnswerOk),
      (SubscribeChannelClient(channel), AnswerOk),
      (PublishChannelClient(channel, m1), Some(NotifyChannelServer(channel, id1))),
      (FetchChannelClient(channel, id1), Some(FetchChannelServer(channel, id1, m1)))
    )

    sendAndVerify(l)

  }

  test("Create a channel, subscribe to it and publish multiple messages") {

    val channel = "main"
    val m = List("Hello on main channel", "Still here", "Is there anyone?")
    val id = m

    val l: List[(JsonMessage, Option[JsonMessage])] =
      List((CreateChannelClient("main", "none"), AnswerOk),
        (SubscribeChannelClient(channel), AnswerOk)
      ) ::: m.zip(id)
        .flatMap { p: (String, String) =>
          List((PublishChannelClient(channel, p._1), Some(NotifyChannelServer(channel, p._2))),
            (FetchChannelClient(channel, p._2), Some(FetchChannelServer(channel, p._2, p._1))))
        }

    sendAndVerify(l)

  }

  test("Create multiple channels, subscribe to them and publish multiple messages") {

    val c1 = "main"
    val c2 = "chan2"
    val m = List("Hello on main channel", "Still here", "Hello chan2", "Is there anyone?", "bye")
    val id = m
    val l = List((CreateChannelClient(c1, "none"), AnswerOk),
      (SubscribeChannelClient(c1), AnswerOk),
      (CreateChannelClient(c2, "none"), AnswerOk),
      (SubscribeChannelClient(c2), AnswerOk),
      (PublishChannelClient(c1, m(0)), Some(NotifyChannelServer(c1, id(0)))),
      (FetchChannelClient(c1, id(0)), Some(FetchChannelServer(c1, id(0), m(0)))),

      (PublishChannelClient(c1, m(1)), Some(NotifyChannelServer(c1, id(1)))),
      (FetchChannelClient(c1, id(1)), Some(FetchChannelServer(c1, id(1), m(1)))),

      (PublishChannelClient(c2, m(2)), Some(NotifyChannelServer(c2, id(2)))),
      (FetchChannelClient(c2, id(2)), Some(FetchChannelServer(c2, id(2), m(2)))),

      (PublishChannelClient(c1, m(3)), Some(NotifyChannelServer(c1, id(3)))),
      (FetchChannelClient(c1, id(3)), Some(FetchChannelServer(c1, id(3), m(3)))),

      (PublishChannelClient(c2, m(4)), Some(NotifyChannelServer(c2, id(4)))),
      (FetchChannelClient(c2, id(4)), Some(FetchChannelServer(c2, id(4), m(4)))),
    )

    sendAndVerify(l)

  }

  test("Two process subscribe and publish on the same channel") {
    val channel = "main"
    val m = List("Hello main!", "message2", "Hello main 2!")
    val id = m
    val l: List[(Option[JsonMessage], Option[JsonMessage], Int)] =
      List((CreateChannelClient(channel, "none"), AnswerOk, 0),
        (SubscribeChannelClient(channel), AnswerOk, 0),
        (SubscribeChannelClient(channel), AnswerOk, 1),
        (PublishChannelClient(channel, m(0)), NotifyChannelServer(channel, id(0)), 0),
        (FetchChannelClient(channel, id(0)), FetchChannelServer(channel, id(0), m(0)), 0),
        (None, NotifyChannelServer(channel, id(0)), 1),
        (FetchChannelClient(channel, id(0)), FetchChannelServer(channel, id(0), m(0)), 1),

        (PublishChannelClient(channel, m(1)), NotifyChannelServer(channel, id(1)), 0),
        (FetchChannelClient(channel, id(1)), FetchChannelServer(channel, id(1), m(1)), 0),
        (None, NotifyChannelServer(channel, id(1)), 1),
        (FetchChannelClient(channel, id(1)), FetchChannelServer(channel, id(1), m(1)), 1),

        (PublishChannelClient(channel, m(2)), NotifyChannelServer(channel, id(2)), 1),
        (FetchChannelClient(channel, id(2)), FetchChannelServer(channel, id(2), m(2)), 1),
        (None, NotifyChannelServer(channel, id(2)), 0),
        (FetchChannelClient(channel, id(2)), FetchChannelServer(channel, id(2), m(2)), 0)

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

  test("Error when fetching an event that does not exist") {
    val l = List((FetchChannelClient("main", "unknown id"),
      Some(AnswerMessageServer(false, Some("Event does not exist")))))
    sendAndVerify(l)
  }

  test("Don't receive messages when unsubscribing from a channel") {
    val channel = "main"
    val m1 = "Hello on main channel"
    val id1 = m1
    val l = List((CreateChannelClient(channel, "none"), AnswerOk),
      (SubscribeChannelClient(channel), AnswerOk),
      (PublishChannelClient(channel, m1), Some(NotifyChannelServer(channel, id1))),
      (FetchChannelClient(channel, id1), Some(FetchChannelServer(channel, id1, m1))),
      (UnsubscribeChannelClient(channel), AnswerOk)
    )

    val root = Behaviors.setup[SpawnProtocol.Command] { context =>
      SpawnProtocol()
    }

    val DatabasePath: String = "database_test"
    val file = new File(DatabasePath)
    //Reset db from previous tests
    val directory = new Directory(file)
    directory.deleteRecursively()

    implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem[SpawnProtocol.Command](root, "test")
    implicit val timeout = Timeout(1, TimeUnit.SECONDS)
    val (entry, actor, dbActor) = setup(DatabasePath)

    val options: Options = new Options()
    options.createIfMissing(true)

    val in = l.map(_._1).flatten
    val out = l.map(_._2).flatten

    val flow = PublishSubscribe.jsonFlow(entry, actor, dbActor).take(out.length)
    val sink: Sink[JsonMessage, Future[Seq[JsonMessage]]] = Sink.fold(Seq.empty[JsonMessage])(_ :+ _)

    val future = Source(in).throttle(1, 200.milli).via(flow).runWith(sink)
    val res = Await.result(future, 5.seconds)
    assert(res == out)
  }

  test("Unsubscribe from a channel you are not subscribed to fails") {
    val l = List((UnsubscribeChannelClient("channel"),
      Some(AnswerMessageServer(false, Some("You are not subscribed to this channel.")))))

    sendAndVerify(l)

  }


}
