import Database.Write
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Sink, Source}

import scala.concurrent.Future
import scala.io.StdIn
import scala.util.{Failure, Success}

object Server {

  /**
   * Create a webserver that handles http requests and websockets requests.
   */
  def main(args: Array[String]): Unit = {

    val root = Behaviors.setup[Nothing]{ context =>
      implicit val system = context.system
      val db = context.spawn(Database(), "db")

      //Route for HTTP request
      val route =
        path("hello") {
          get {
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
          }
        }

      //Route for websocket requests
      def wsRoute = path("ws") {
        //Reply to a message with the content of the received message
        handleWebSocketMessages(
          Flow[Message].collect {
            case TextMessage.Strict(text) =>
              db ! Write(text, text)
              TextMessage("You said " + text)
          }
        )
      }

      implicit val executionContext = system.executionContext
      val bindingFuture = Http().newServerAt("localhost", 8080).bind(route ~ wsRoute)
      bindingFuture.onComplete {
        case Success(value) => println("Server online at http://localhost:8080/")
        case Failure(exception) => println("Server failed to start")
          system.terminate()
      }

      Behaviors.empty
    }

    val system = ActorSystem[Nothing](root, "pop")

    StdIn.readLine() // let it run until user presses return

  }
}

object Database {
  sealed trait DBAction
  final case class Write(key: String, value : String) extends DBAction
  def apply(): Behavior[DBAction]  = db()

  private def db(): Behavior[DBAction] =
    Behaviors.receiveMessage {
      case Write(key, value) =>
        //TODO: write in the database here
        println(key + ": " + value)
        Behaviors.same
    }
}