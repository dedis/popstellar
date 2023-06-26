package ch.epfl.pop

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import akka.pattern.AskableActorRef
import akka.util.Timeout
import ch.epfl.pop.authentication.{GetRequestHandler, WebSocketResponseHandler}
import ch.epfl.pop.config.RuntimeEnvironment
import ch.epfl.pop.config.RuntimeEnvironment._
import ch.epfl.pop.decentralized.{ConnectionMediator, HeartbeatGenerator, Monitor}
import ch.epfl.pop.pubsub.{MessageRegistry, PubSubMediator, PublishSubscribe}
import ch.epfl.pop.storage.{DbActor, SecurityModuleActor}
import org.iq80.leveldb.Options

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

object Server {
  /*
   * Create a WebServer that handles http requests and WebSockets requests.
   */
  def main(args: Array[String]): Unit = {
    /* Get Setup configuration*/
    println("Loading configuration from file...")
    val appConf = RuntimeEnvironment.appConf

    val system = akka.actor.ActorSystem("pop-be2-inner-actor-system", appConf)

    implicit val typedSystem: ActorSystem[Nothing] = system.toTyped
    val logger = system.log

    val root = Behaviors.setup[Nothing] { _ =>
      implicit val timeout: Timeout = Timeout(1, TimeUnit.SECONDS)
      // Create database
      val options: Options = new Options()
      options.createIfMissing(true)

      val messageRegistry: MessageRegistry = MessageRegistry()
      val pubSubMediatorRef: ActorRef = system.actorOf(PubSubMediator.props, "PubSubMediator")
      
      val dbActorRef: AskableActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, messageRegistry)), "DbActor")
      val securityModuleActorRef: AskableActorRef = system.actorOf(Props(SecurityModuleActor(RuntimeEnvironment.securityPath)))

      // Create necessary actors for server-server communications
      val heartbeatGenRef: ActorRef = system.actorOf(HeartbeatGenerator.props(dbActorRef))
      val monitorRef: ActorRef = system.actorOf(Monitor.props(heartbeatGenRef))
      val connectionMediatorRef: ActorRef = system.actorOf(ConnectionMediator.props(monitorRef, pubSubMediatorRef, dbActorRef, securityModuleActorRef, messageRegistry))

      // Setup routes
      def publishSubscribeRoute: RequestContext => Future[RouteResult] = {
        path(serverConf.clientPath) {
          handleWebSocketMessages(
            PublishSubscribe.buildGraph(
              pubSubMediatorRef,
              dbActorRef,
              securityModuleActorRef,
              messageRegistry,
              monitorRef,
              connectionMediatorRef,
              isServer = false
            )(system)
          )
        } ~ path(serverConf.serverPath) {
          handleWebSocketMessages(
            PublishSubscribe.buildGraph(
              pubSubMediatorRef,
              dbActorRef,
              securityModuleActorRef,
              messageRegistry,
              monitorRef,
              connectionMediatorRef,
              isServer = true
            )(system)
          )
        }
      }

      def getRequestsRoute = GetRequestHandler.buildRoutes(serverConf, securityModuleActorRef)

      def authenticateWsResponseRoute = WebSocketResponseHandler.buildRoute(serverConf)(system)

      def allRoutes = concat(
        authenticateWsResponseRoute,
        getRequestsRoute,
        publishSubscribeRoute
      )

      implicit val executionContext: ExecutionContextExecutor = typedSystem.executionContext
      /* Setup http server with bind and route config*/
      val bindingFuture = Http().newServerAt(serverConf.interface, serverConf.port).bindFlow(allRoutes)

      bindingFuture.onComplete {
        case Success(_) =>
          println(f"[Client] ch.epfl.pop.Server online at $ownClientAddress")
          println(f"[Server] ch.epfl.pop.Server online at $ownServerAddress")
          println(f"[Server] ch.epfl.pop.Server auth server online at $ownAuthAddress")
          println(f"[Server] ch.epfl.pop.Server auth ws server online at $ownResponseAddress")
          println(f"[Server] ch.epfl.pop.Server public key available at $ownPublicKeyAddress")

        case Failure(_) =>
          logger.error(
            "ch.epfl.pop.Server failed to start. Terminating actor system"
          )
          system.terminate()
          typedSystem.terminate()
      }
      /* Shutting down */
      val shutdownListener = new Thread() {
        override def run(): Unit = {
          try {
            bindingFuture.flatMap(_.unbind()).onComplete(_ => { // trigger unbinding from the port
              logger.info("Server terminated !")
              system.terminate()
              typedSystem.terminate()
            }) // and shutdown when done
          } catch {
            case NonFatal(e) => logger.warning(s"Server shutting thread was interrupted : ${e.getMessage}")
          }
        }
      }
      Runtime.getRuntime.addShutdownHook(shutdownListener)
      Behaviors.empty
    }
    // Deploys system actor with root behavior
    ActorSystem[Nothing](root, "pop-be2-actor-system")

  }
}
