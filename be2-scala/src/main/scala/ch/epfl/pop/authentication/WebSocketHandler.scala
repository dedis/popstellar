package ch.epfl.pop.authentication

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{CompletionStrategy, OverflowStrategy}
import akka.{Done, NotUsed}
import ch.epfl.pop.config.ServerConf

import scala.concurrent.Future

object WebSocketHandler {

  private val LISTENER_BUFFER_SIZE: Int = 256

  private val socketsConnected: collection.mutable.Map[(String, String, String), ActorRef] = collection.mutable.Map()

  def buildRoute(config: ServerConf)(implicit system: ActorSystem): Route = {
    path(config.authenticationPath / config.authenticationResponseEndpoint / Segment / Segment / Segment) {
      (laoId: String, clientId: String, nonce: String) =>
        handleWebSocketMessages(handleMessage(laoId, clientId, nonce))
    }
  }

  private def handleMessage(laoId: String, clientId: String, nonce: String)(implicit system: ActorSystem): Flow[Message, Message, Any] = {
    val socketId = (laoId, clientId, nonce)
    socketsConnected.get(socketId) match {
      case Some(listener) => handleMessageAsServer(listener, socketId)
      case None           => handleMessageAsListener(socketId)
    }
  }

  private def handleMessageAsListener(socketId: (String, String, String)): Flow[Message, Message, Any] = {
    println("received message from listener")
    val dummySink = Sink.ignore
    val source: Source[TextMessage, NotUsed] = Source
      .actorRef(
        {
          case akka.actor.Status.Success(s: CompletionStrategy) => s
          case akka.actor.Status.Success(_)                     => CompletionStrategy.draining
          case akka.actor.Status.Success                        => CompletionStrategy.draining
        },
        {
          case akka.actor.Status.Failure(cause) => cause
        },
        bufferSize = LISTENER_BUFFER_SIZE,
        overflowStrategy = OverflowStrategy.dropBuffer // OverflowStrategy back-pressure is not allowed!
      )
      .mapMaterializedValue(wsHandle => {
        socketsConnected.put(socketId, wsHandle)
        NotUsed
      })
    Flow.fromSinkAndSource(dummySink, source)
  }

  private def handleMessageAsServer(listenerRef: ActorRef, socketId: (String, String, String)): Flow[Message, Message, Any] = {
    println("received message from server")
    val sink: Sink[Message, Future[Done]] =
      Sink.foreach {
        case message: TextMessage.Strict =>
          println(message.text)
          listenerRef ! message
          socketsConnected.remove(socketId)
        case _ => // ignore other message types
      }
    val dummySource: Source[Message, NotUsed] = Source.empty
    Flow.fromSinkAndSource(sink, dummySource)
  }
}
