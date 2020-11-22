package ch.epfl.pop

import java.util.concurrent.TimeUnit

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub}
import akka.util.Timeout
import ch.epfl.pop.json.JsonMessages.PropagateMessageClient
import ch.epfl.pop.pubsub.{ChannelActor, PublishSubscribe}
import org.iq80.leveldb.Options

import scala.io.StdIn
import scala.util.{Failure, Success}

object Server {

  /**
   * Create a webserver that handles http requests and websockets requests.
   */
  def main(args: Array[String]): Unit = {

    val root = Behaviors.setup[Nothing] { context =>
      implicit val system = context.system

      //Route for HTTP request
      val route =
        path("hello") {
          get {
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
          }
        }

      //Stream that send all published messages to all clients
      val (publishEntry, subscribeExit) = MergeHub.source[PropagateMessageClient].toMat(BroadcastHub.sink)(Keep.both).run()
      implicit val timeout = Timeout(1, TimeUnit.SECONDS)
      val actor = context.spawn(ChannelActor(subscribeExit), "actor")
      //Create database
      val options: Options = new Options()
      options.createIfMissing(true)
      val DatabasePath: String = "database"
      val dbActor = context.spawn(DBActor(DatabasePath, publishEntry), "actorDB")

      def publishSubscribeRoute = path("ps") {
        handleWebSocketMessages(PublishSubscribe.messageFlow(actor, dbActor))
      }

      implicit val executionContext = system.executionContext
      val bindingFuture = Http().newServerAt("localhost", 8080).bind(route ~ publishSubscribeRoute)
      bindingFuture.onComplete {
        case Success(value) => println("ch.epfl.pop.Server online at http://localhost:8080/")
        case Failure(exception) => println("ch.epfl.pop.Server failed to start")
          system.terminate()
      }

      Behaviors.empty
    }

    val system = ActorSystem[Nothing](root, "pop")

    StdIn.readLine() // let it run until user presses return

  }
}