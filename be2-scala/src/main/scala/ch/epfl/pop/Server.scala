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
import com.typesafe.config.ConfigFactory
import java.io.File
import ch.epfl.pop.config.RuntimeEnvironment
import ch.epfl.pop.config.ServerConf
import akka.event.LoggingAdapter

object Server {

  /** Create a WebServer that handles http requests and WebSockets requests.
    */
  def main(args: Array[String]): Unit = {

    /* Get configuration object for akka actor/http*/
    val appConf = RuntimeEnvironment.appConf

    /* Get Setup configuration*/ 
    println("Loading configuration from file...")
    val config = ServerConf(appConf)
   
    val system = akka.actor.ActorSystem("pop-be2-inner-actor-system", appConf)
    
    implicit val typedSystem: ActorSystem[Nothing] = system.toTyped
    val logger = system.log

    val root = Behaviors.setup[Nothing] { _ =>
      implicit val timeout: Timeout = Timeout(1, TimeUnit.SECONDS)
      // Create database
      val options: Options = new Options()
      options.createIfMissing(true)

      val pubSubMediatorRef: ActorRef = system.actorOf(PubSubMediator.props, "PubSubMediator")
      val dbActorRef: AskableActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef)), "DbActor")

      def publishSubscribeRoute: RequestContext => Future[RouteResult] = path(config.path) {
        handleWebSocketMessages(PublishSubscribe.buildGraph(pubSubMediatorRef, dbActorRef)(system))
      }
     
      implicit val executionContext: ExecutionContextExecutor = typedSystem.executionContext
      /* Setup http server with bind and route config*/ 
      val bindingFuture = Http().bindAndHandle(publishSubscribeRoute, config.interface, config.port)

      bindingFuture.onComplete {
        case Success(_) => println(f"ch.epfl.pop.Server online at ws://${config.interface}:${config.port}/${config.path}")
        case Failure(_) =>
          logger.error(
            "ch.epfl.pop.Server failed to start. Terminating actor system"
          )
          system.terminate()
          typedSystem.terminate()
      }

    /***Shutting down**/
    val shutdownListener = new Thread(){
              override def run(): Unit ={
                      logger.warning("shutdown in 5s ");
                      try {
                          Thread.sleep(5000);
                            bindingFuture
              .flatMap(_.unbind()) // trigger unbinding from the port
              .onComplete(_ => {
                logger.info("Server terminated !")
                system.terminate()
                typedSystem.terminate()
              }) // and shutdown when done
                      } catch {
                          case  e: InterruptedException =>   logger.warning("Server shutting thread was interrupted !")    

                      }
                  }
              };
              Runtime.getRuntime().addShutdownHook(shutdownListener);

      Behaviors.empty
    }

    //Deploys system actor with root behavior
    ActorSystem[Nothing](root, "pop-be2-actor-system")

  }
}

