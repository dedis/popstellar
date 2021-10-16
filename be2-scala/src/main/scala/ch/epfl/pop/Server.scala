package ch.epfl.pop

import java.util.concurrent.TimeUnit

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import akka.pattern.AskableActorRef
import akka.util.Timeout
import ch.epfl.pop.pubsub.graph.DbActor
import ch.epfl.pop.pubsub.{PubSubMediator, PublishSubscribe}
import org.iq80.leveldb.Options

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn
import scala.util.{Failure, Success}

object Server {
  private final def PORT: Int = 8000

  private final def PATH: String = ""

  /** Create a WebServer that handles http requests and WebSockets requests.
    */
  def main(args: Array[String]): Unit = {

    val system = akka.actor.ActorSystem("pop-be2-inner-actor-system")
    implicit val typedSystem: ActorSystem[Nothing] = system.toTyped
    val logger = system.log

    val root = Behaviors.setup[Nothing] { _ =>
      implicit val timeout: Timeout = Timeout(1, TimeUnit.SECONDS)
      // Create database
      val options: Options = new Options()
      options.createIfMissing(true)

      val pubSubMediatorRef: ActorRef = system.actorOf(PubSubMediator.props, "PubSubMediator")
      val dbActorRef: AskableActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef)), "DbActor")

      def publishSubscribeRoute: RequestContext => Future[RouteResult] = path(PATH) {
        handleWebSocketMessages(PublishSubscribe.buildGraph(pubSubMediatorRef, dbActorRef)(system))
      }
     
      implicit val executionContext: ExecutionContextExecutor =
        typedSystem.executionContext
      val bindingFuture = Http()
        .newServerAt("localhost", PORT)
        .bind(publishSubscribeRoute)

      bindingFuture.onComplete {
        case Success(_) =>
          logger.info(
            "ch.epfl.pop.Server online at ws://localhost:{}/{}\nPress RETURN to stop",
            PORT,
            PATH
          )

        case Failure(_) =>
          logger.error(
            "ch.epfl.pop.Server failed to start. Terminating actor system"
          )
          system.terminate()
          typedSystem.terminate()
      }

      /* Server terminating logic */
      StdIn.readLine // let it run until user presses return
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => {
          logger.info("Server terminated !")
          system.terminate()
          typedSystem.terminate()
        }) // and shutdown when done

      Behaviors.empty
    }

    //Deploys system actor with root behavior
    ActorSystem[Nothing](root, "pop-be2-actor-system")

  }
}
