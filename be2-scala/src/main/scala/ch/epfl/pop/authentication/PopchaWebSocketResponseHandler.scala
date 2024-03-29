package ch.epfl.pop.authentication

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{CompletionStrategy, OverflowStrategy}
import akka.{Done, NotUsed}
import ch.epfl.pop.config.ServerConf

import scala.concurrent.{Future, Promise}

/** This websocket handler receives the JWT from the PopchaHandler via websocket and forwards it to the client webpage connected via websocket as well.
  */
object PopchaWebSocketResponseHandler {

  private val LISTENER_BUFFER_SIZE: Int = 256

  private val socketsConnected: collection.mutable.Map[(String, String, String), ActorRef] = collection.mutable.Map()

  /** Builds a route to handle the websocket requests received
    * @param config
    *   server configuration to use
    * @param system
    *   actor system to use to spawn actors
    * @return
    *   a route that handles the server's response websocket messages
    */
  def buildRoute(config: ServerConf)(implicit system: ActorSystem): Route = {
    path(config.responseEndpoint / Segment / "authentication" / Segment / Segment) {
      (laoId: String, clientId: String, nonce: String) =>
        handleWebSocketMessages(handleMessage(laoId, clientId, nonce))
    }
  }

  /** Handle a message received using its socket id (laoId, clientId, nonce):
    *   - If it is the first connection on this socket id, the connection is handled as a listener (waiting for a message).
    *   - If it is the second connection on this socket id, the connection is handled as a server sending messages to the listener waiting.
    */
  private def handleMessage(laoId: String, clientId: String, nonce: String)(implicit system: ActorSystem): Flow[Message, Message, Any] = {
    val socketId = (laoId, clientId, nonce)
    socketsConnected.get(socketId) match {
      case Some(listener) => handleMessageAsServer(listener, socketId)
      case None           => handleMessageAsListener(socketId)
    }
  }

  private def handleMessageAsListener(socketId: (String, String, String)): Flow[Message, Message, Any] = {
    val dummySink = Sink.ignore
    val source: Source[TextMessage, NotUsed] = Source
      .actorRef(
        {
          case Done => CompletionStrategy.immediately
        },
        PartialFunction.empty,
        bufferSize = LISTENER_BUFFER_SIZE,
        overflowStrategy = OverflowStrategy.dropBuffer
      )
      .mapMaterializedValue(wsHandle => {
        socketsConnected.put(socketId, wsHandle)
        NotUsed
      })
    Flow.fromSinkAndSourceCoupled(dummySink, source)
  }

  private def handleMessageAsServer(listenerRef: ActorRef, socketId: (String, String, String)): Flow[Message, Message, Any] = {
    val sink: Sink[Message, Future[Done]] =
      Sink.foreach {
        case message: TextMessage.Strict =>
          listenerRef ! message
          listenerRef ! Done
          socketsConnected.remove(socketId)
        case _ => // ignore other message types
      }
    val dummySource: Source[Message, Promise[Option[Nothing]]] = Source.maybe
    Flow.fromSinkAndSourceCoupled(sink, dummySource)
  }
}
