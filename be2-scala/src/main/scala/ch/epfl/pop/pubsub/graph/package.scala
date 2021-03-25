package ch.epfl.pop.pubsub

import ch.epfl.pop.model.network.{JsonRpcMessage, JsonRpcResponse}

package object graph {
  type JsonString = String
  type GraphMessage = Either[JsonRpcMessage, PipelineError]
}
