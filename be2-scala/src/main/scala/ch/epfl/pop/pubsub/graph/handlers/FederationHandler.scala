package ch.epfl.pop.pubsub.graph.handlers

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.pubsub.graph.GraphMessage

object FederationHandler extends MessageHandler {
  final lazy val handlerInstance = new FederationHandler(super.dbActor)
  
  def handleFederationChallenge(rpcMessage: JsonRpcRequest) : GraphMessage = handlerInstance.handleFederationChallenge(rpcMessage)
  
  def handleFederationRequestChallenge(rpcMessage: JsonRpcRequest) : GraphMessage = handlerInstance.handleFederationRequestChallenge(rpcMessage)
  
  def handleFederationInit(rpcMessage: JsonRpcRequest) : GraphMessage = handlerInstance.handleFederationInit(rpcMessage)
  
  def handleFederationExpect(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleFederationExpect(rpcMessage)
  
  def handleFederationResult(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleFederationResult(rpcMessage)

}

class FederationHandler(dbRef: => AskableActorRef) extends MessageHandler{
  
  override final val dbActor: AskableActorRef = dbRef
  def handleFederationChallenge(rpcMessage : JsonRpcRequest): GraphMessage ={
    
  }
  def handleFederationRequestChallenge(rpcMessage: JsonRpcRequest) : GraphMessage ={}
  def handleFederationInit(rpcMessage: JsonRpcRequest): GraphMessage ={
    
  }
  def handleFederationExpect(rpcMessage: JsonRpcRequest): GraphMessage ={}

  def handleFederationResult(rpcMessage: JsonRpcRequest): GraphMessage ={} 
}
