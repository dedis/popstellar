package ch.epfl.pop.pubsub.graph

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.json.JsonMessageParser
import ch.epfl.pop.model.network.JsonRpcMessage

import scala.util.Try

object MessageEncoder {

  val serializer: Flow[JsonRpcMessage, Nothing, NotUsed] = Flow[JsonRpcMessage].map { message =>
    val serializedMessage: Try[String] = Try(JsonMessageParser.serializeMessage(message))
    serializedMessage match {
      case _ => ??? // Success/Failure
      // return un TextMessage.strict(str)
    }
  }
}
