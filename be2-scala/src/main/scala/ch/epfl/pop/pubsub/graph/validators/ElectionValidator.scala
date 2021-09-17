package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.pubsub.graph.GraphMessage

object ElectionValidator extends MessageDataContentValidator {
  def validateSetupElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    // FIXME election stuff
    Left(rpcMessage)
  }

  def validateResultElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    // FIXME election stuff
    Left(rpcMessage)
  }

  def validateEndElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    // FIXME election stuff
    Left(rpcMessage)
  }
}
