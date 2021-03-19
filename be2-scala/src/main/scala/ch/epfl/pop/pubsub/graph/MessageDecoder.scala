package ch.epfl.pop.pubsub.graph

import akka.NotUsed
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.Flow
import ch.epfl.pop.json.JsonMessageParser

object MessageDecoder {

  // Message or TextMessage
  val jsonRpcParser: Flow[Message, Nothing, NotUsed] = Flow[Message].map {
    case TextMessage.Strict(text) => JsonMessageParser.parseMessage(text) match {
      case _ => ???
    }
    case TextMessage.Streamed(stream) => ??? // throw
  }

  val messageParser = ???
}
