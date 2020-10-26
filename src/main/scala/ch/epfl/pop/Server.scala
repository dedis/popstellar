package ch.epfl.pop

import java.util.concurrent.TimeUnit

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Sink}
import akka.util.Timeout
import ch.epfl.pop.json.JsonMessageParser.{parseMessage, serializeMessage}
import ch.epfl.pop.json.JsonMessages.{FetchChannelServer, JsonMessage, PublishChannelClient}
import ch.epfl.pop.pubsub.{ChannelActor, PublishSubscribe}

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
      val (hsink, hsource) = MergeHub.source[PublishChannelClient].toMat(BroadcastHub.sink)(Keep.both).run()
      implicit val timeout = Timeout(1, TimeUnit.SECONDS)
      val actor = context.spawn(ChannelActor(hsource), "actor")

      def publishSubscribeRoute = path("ps") {
        val parser = Flow[Message].map {
          case TextMessage.Strict(s) => parseMessage(s)
        }
        val formatter = Flow[JsonMessage].map {
          case PublishChannelClient(channel, event) =>
            //Curently we transform published messages to Fetch messages as notifications are not implemented
            FetchChannelServer(channel, event, "0")
          case x => x
        }
          .map(m => TextMessage.Strict(serializeMessage(m)))
        val jsonFlow = PublishSubscribe.getFlow(hsink, actor)

        handleWebSocketMessages(parser.via(jsonFlow).via(formatter))
      }

      implicit val executionContext = system.executionContext
      val bindingFuture = Http().newServerAt("localhost", 8080).bind(route ~  publishSubscribeRoute)
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