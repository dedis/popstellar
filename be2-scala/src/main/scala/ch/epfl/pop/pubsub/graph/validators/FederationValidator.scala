package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.pubsub.graph.GraphMessage

object FederationValidator extends MessageDataContentValidator  with EventValidator {
  
  def validateFederationChallenge(rpcMessage: JsonRpcRequest): GraphMessage =
  def validateFederationRequestChallenge(rpcMessage: JsonRpcRequest): GraphMessage =
  def validateFederationInit(rpcMessage: JsonRpcRequest): GraphMessage =
  def validateFederationExpect(rpcMessage: JsonRpcRequest): GraphMessage =
  def validateFederationResult(rpcMessage: JsonRpcRequest): GraphMessage =   

}

sealed class FederationValidator(dbActorRef: => AskableActorRef) extends MessageDataContentValidator with EventValidator{
  
}
