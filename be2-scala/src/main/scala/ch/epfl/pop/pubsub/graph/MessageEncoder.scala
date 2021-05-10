package ch.epfl.pop.pubsub.graph

import akka.NotUsed
import akka.stream.scaladsl.Flow

object MessageEncoder {

  def serializeMessage(graphMessage: GraphMessage): GraphMessage = graphMessage match {
    // Note: the output message (if successful) is an answer
    case Left(jsonRpcMessage) => Left(???) // FIXME generate json rpc answer with id corresponding to the one in the input rpc message
    case _ => graphMessage // propagate the pipeline error one final time
  }

  val serializer: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map(serializeMessage)
}
