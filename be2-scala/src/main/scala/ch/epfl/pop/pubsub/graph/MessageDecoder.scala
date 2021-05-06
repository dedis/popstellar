package ch.epfl.pop.pubsub.graph

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.{JsonRpcRequest, MethodType}

object MessageDecoder {

  val jsonRpcParser: Flow[Either[JsonString, PipelineError], GraphMessage, NotUsed] = Flow[Either[JsonString, PipelineError]].map {
    case _ => Right(PipelineError(-12, "json rpc parser output")) // FIXME temporary while parser branch is not mrgered
    case Left(jsonString) =>
      Left(JsonRpcRequest("2.0", MethodType.INVALID, ???, Some(0)))
    // case _ => _ // TODO check if type ok. Else prob Right(pipelineError)
  }

  val messageParser: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map(parseMessage)

  def parseMessage(graphMessage: GraphMessage): GraphMessage = graphMessage
}
