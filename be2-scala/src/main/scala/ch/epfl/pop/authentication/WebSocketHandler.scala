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
          println(message.text)
          listenerRef ! message
          listenerRef ! Done
          socketsConnected.remove(socketId)
        case _ => // ignore other message types
      }
    val dummySource: Source[Message, Promise[Option[Nothing]]] = Source.maybe
    Flow.fromSinkAndSourceCoupled(sink, dummySource)
  }
}
