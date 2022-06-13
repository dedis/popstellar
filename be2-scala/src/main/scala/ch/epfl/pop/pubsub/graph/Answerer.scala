package ch.epfl.pop.pubsub.graph

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.TextMessage
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{CompletionStrategy, OverflowStrategy}
import ch.epfl.pop.json.HighLevelProtocol._
import ch.epfl.pop.model.network.{ErrorObject, JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.pubsub.ClientActor.{ClientAnswer, ConnectWsHandle, DisconnectWsHandle}
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import spray.json._

object Answerer {
  private val CLIENT_BUFFER_SIZE: Int = 256

  private def errorResponseString(code: Int, reason: String, rpcId: Option[Int]): String = JsonRpcResponse(
    RpcValidator.JSON_RPC_VERSION,
    None,
    Some(ErrorObject(code, reason)),
    rpcId
  ).toJson.toString

  /** Send an answer back to the client (associated to clientActorRef from the answerer)
    *
    * @param graphMessage
    *   terminal pipeline message
    * @return
    *   the message sent
    */
  private def sendAnswer(graphMessage: GraphMessage): TextMessage = graphMessage match {
    // Note: The encoding of the answer is done here as the ClientActor must always receive a GraphMessage
    case Left(rpcAnswer: JsonRpcResponse)    => TextMessage.Strict(rpcAnswer.toJson.toString)
    case Left(rpcRequest: JsonRpcRequest)    => TextMessage.Strict(rpcRequest.toJson.toString) // propagate server
    case Right(pipelineError: PipelineError) =>
      // Convert AnswerGenerator's PipelineErrors into negative JsonRpcResponses and send them back to the client
      TextMessage.Strict(errorResponseString(pipelineError.code, pipelineError.description, pipelineError.rpcId))
    case m @ _ =>
      println(s"An unknown error occurred in Answerer.sendAnswer, unexpected input: $m")
      TextMessage.Strict(errorResponseString(ErrorCodes.SERVER_ERROR.id, "Unknown internal server state", None))
  }

  def answerer(clientActorRef: ActorRef, mediator: ActorRef)(implicit system: ActorSystem): Flow[GraphMessage, TextMessage, NotUsed] = {

    // Integration point between Akka Streams and the above actor
    val sink: Sink[GraphMessage, NotUsed] = Flow[GraphMessage]
      // Create a ClientAnswer from the input graph message
      .collect { case graphMessage: GraphMessage => ClientAnswer(graphMessage) }
      // Send the ClientAnswer to clientActorRef. Whenever the stream between the client
      // actor and the actual client (front-end) is broken, the message DisconnectWsHandle
      // is sent to clientActorRef
      .to(Sink.actorRef(
        clientActorRef,
        DisconnectWsHandle,
        { (t: Throwable) =>
          println(t); DisconnectWsHandle
        }
      ))

    // Integration point between Akka Streams and above actor
    val source: Source[TextMessage, NotUsed] = Source
      // By using .actorRef, the source emits whatever the actor "wsHandle" sends
      .actorRef(
        {
          case akka.actor.Status.Success(s: CompletionStrategy) => s
          case akka.actor.Status.Success(_)                     => CompletionStrategy.draining
          case akka.actor.Status.Success                        => CompletionStrategy.draining
        },
        {
          case akka.actor.Status.Failure(cause) => cause
        },
        bufferSize = CLIENT_BUFFER_SIZE,
        overflowStrategy = OverflowStrategy.dropBuffer // OverflowStrategy back-pressure is not allowed!
      )
      // Send an answer back to the client (the one represented by wsHandle == clientActorRef)
      .map((graphMessage: GraphMessage) => sendAnswer(graphMessage))
      .mapMaterializedValue { wsHandle =>
        // The wsHandle is the way to talk back to the user, our client actor needs to know
        // about this to send messages to the WebSocket user
        clientActorRef ! ConnectWsHandle(wsHandle)
        // Don't expose the wsHandle anymore
        NotUsed
      }

    // Combines a sink (input) and a source (output) to create a fake flow (one input and one
    // input) even though there's no link between sink and source
    // So when the emitted Flow gets cancellation the source will be completed
    // and so does the the sink
    Flow.fromSinkAndSourceCoupled(sink, source)
  }
}
