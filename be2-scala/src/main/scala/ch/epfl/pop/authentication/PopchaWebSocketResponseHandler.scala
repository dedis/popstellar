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

/** Websocket handler that, given two websocket connections on a common endpoint, forwards a single message from the second websocket connected to the first:
  *   - Connection requests are expected to lend on endpoints with format "/laoId/authentication/clientId/nonce". This uniquely determines a connection by its socketId (laoId, clientId, nonce).
  *   - The first websocket connected on a given endpoint is assumed to be waiting for a message. It will be provided by the second websocket to connect on that same endpoint (at any point later in time).
  *   - Once a message is received from the second websocket connected on a given endpoint, it is forwarded to the first websocket connected and both connections are then closed.
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
