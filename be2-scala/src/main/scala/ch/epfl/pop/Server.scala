package ch.epfl.pop

import java.util.concurrent.TimeUnit

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub}
import akka.util.Timeout
import ch.epfl.pop.json.JsonMessages.PropagateMessageServer
import ch.epfl.pop.pubsub.{ChannelActor, PublishSubscribe}
import org.iq80.leveldb.Options

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn
import scala.util.{Failure, Success}

object Server {

  /**
   * Create a webserver that handles http requests and websockets requests.
   */
  def main(args: Array[String]): Unit = {
    val PORT = 8000
    val PATH = ""

    val root = Behaviors.setup[Nothing] { context =>
      implicit val system: ActorSystem[Nothing] = context.system

      //Stream that send all published messages to all clients
      val (publishEntry, subscribeExit) = MergeHub.source[PropagateMessageServer].toMat(BroadcastHub.sink)(Keep.both).run()
      implicit val timeout: Timeout = Timeout(1, TimeUnit.SECONDS)
      val actor = context.spawn(ChannelActor(subscribeExit), "actor")
      //Create database
      val options: Options = new Options()
      options.createIfMissing(true)
      val DatabasePath: String = "database"
      val dbActor = context.spawn(DBActor(DatabasePath), "actorDB")

      def publishSubscribeRoute = path(PATH) {
        handleWebSocketMessages(PublishSubscribe.messageFlow(actor, dbActor)(timeout, system, publishEntry))
      }

      implicit val executionContext: ExecutionContextExecutor = system.executionContext
      val bindingFuture = Http().newServerAt("localhost", PORT).bind(publishSubscribeRoute)
      bindingFuture.onComplete {
        case Success(value) => println("ch.epfl.pop.Server online at ws://localhost:" + PORT + "/" + PATH)
        case Failure(exception) => println("ch.epfl.pop.Server failed to start")
          system.terminate()
      }

      Behaviors.empty
    }

    val system = ActorSystem[Nothing](root, "pop")

    StdIn.readLine() // let it run until user presses return

  }
}
