package ch.epfl.pop.pubsub

import ch.epfl.pop.model.network.JsonRpcMessage

package object graph {
  type JsonString = String
  type GraphMessage = Either[PipelineError, JsonRpcMessage] // FIXME convention states that Left(_) is the success value
}
