package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Partition, Sink, Source}
import ch.epfl.pop.model.network.method.Publish
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse, MethodType}
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.pubsub.graph.{GraphMessage, Validator}
import ch.epfl.pop.pubsub.{AskPatternConstants, ClientActor, MessageRegistry}

import scala.annotation.tailrec

//This object's job is to handle responses it receives from other servers after sending a heartbeat.
// When receiving the missing messages, the server's job is to write them on the database.
object GetMessagesByIdResponseHandler extends AskPatternConstants {

  private final val MAX_RETRY = 5

  def graph(mediatorActorRef: ActorRef, messageRegistry: MessageRegistry)(implicit system: ActorSystem): Flow[GraphMessage, GraphMessage, NotUsed] =
    Flow.fromGraph(GraphDSL.create() {
      implicit builder: GraphDSL.Builder[NotUsed] =>
        {
          import GraphDSL.Implicits._

          /* partitioner port numbers */
          val portPipelineError = 0
          val portResponseHandler = 1
          val totalPorts = 2

          /* building blocks */
          val handlerPartitioner = builder.add(Partition[GraphMessage](
            totalPorts,
            {
              case Right(jsonRpcMessage: JsonRpcResponse) => jsonRpcMessage.result match {
                  case Some(result) => result.resultMap match {
                      case Some(_) => portResponseHandler
                      case _       => portPipelineError
                    }
                  case _ => portPipelineError
                }
              case _ => portPipelineError // Pipeline error goes directly in handlerMerger
            }
          ))

          val responseHandler = builder.add(GetMessagesByIdResponseHandler.responseHandler(mediatorActorRef, messageRegistry)(system))
          val handlerMerger = builder.add(Merge[GraphMessage](totalPorts))

          /* glue the components together */
          handlerPartitioner.out(portPipelineError) ~> handlerMerger
          handlerPartitioner.out(portResponseHandler) ~> responseHandler ~> handlerMerger

          /* close the shape */
          FlowShape(handlerPartitioner.in, handlerMerger.out)
        }
    })
  private def validator(
      mediatorActorRef: ActorRef,
      messageRegistry: MessageRegistry
  )(implicit system: ActorSystem): Flow[GraphMessage, GraphMessage, NotUsed] =
    Flow.fromGraph(GraphDSL.create() {
      implicit builder: GraphDSL.Builder[NotUsed] =>
        {
          import GraphDSL.Implicits._

          val input = builder.add(Flow[GraphMessage].collect { case msg: GraphMessage => msg })

          val localClient: ActorRef = system.actorOf(ClientActor.props(mediatorActorRef, ActorRef.noSender, isServer = false))

          /* partitioner port numbers */
          val portPipelineError = 0
          val portParamsWithMessage = 1
          val portParamsWithChannel = 2
          val totalPorts = 3

          val methodPartitioner = builder.add(Partition[GraphMessage](
            totalPorts,
            {
              case Right(m: JsonRpcRequest) if m.hasParamsMessage => portParamsWithMessage // Publish and Broadcast messages
              case Right(m: JsonRpcRequest) if m.hasParamsChannel => portParamsWithChannel
              case _                                              => portPipelineError // Pipeline error goes directly in merger
            }
          ))

          val jsonRpcContentValidator = builder.add(Validator.jsonRpcContentValidator)
          val hasMessagePartition = builder.add(ParamsWithMessageHandler.graph(messageRegistry))
          val hasChannelPartition = builder.add(ParamsWithChannelHandler.graph(localClient))

          val merger = builder.add(Merge[GraphMessage](totalPorts))

          // output message (answer) for the client
          val output = builder.add(Flow[GraphMessage])

          /* glue the components together */
          input ~> jsonRpcContentValidator ~> methodPartitioner

          methodPartitioner.out(portPipelineError) ~> merger
          methodPartitioner.out(portParamsWithMessage) ~> hasMessagePartition ~> merger
          methodPartitioner.out(portParamsWithChannel) ~> hasChannelPartition ~> merger
          merger ~> output

          /* close the shape */
          FlowShape(input.in, output.out)
        }
    })

  private def responseHandler(mediatorActorRef: ActorRef, messageRegistry: MessageRegistry)(implicit system: ActorSystem): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcMessage: JsonRpcResponse) =>
      val receivedResponse = jsonRpcMessage.result.get.resultMap.get
      val validatorFlow = validator(mediatorActorRef, messageRegistry)
      receivedResponse.foreach {
        case (channel, messagesSet) =>
          val publishMessagesSet: Set[GraphMessage] = messagesSet.map(message =>
            Right(JsonRpcRequest(RpcValidator.JSON_RPC_VERSION, MethodType.PUBLISH, new Publish(channel, message), Some(0)))
          )
          passThroughPipeline(validatorFlow, publishMessagesSet, MAX_RETRY)
      }
      Right(jsonRpcMessage)
    case value @ _ => value
  }.filter(_ => false) // instead of implementing a Sink[GraphMessage], we chose to implement it as a Flow and filter every single outgoing graphMessage

  @tailrec
  private def passThroughPipeline(validator: Flow[GraphMessage, GraphMessage, NotUsed], messageSet: Set[GraphMessage], remainingAttempts: Int)(implicit system: ActorSystem): Unit = {
    if (remainingAttempts <= 0)
      return

    var failedGraphMessages: Set[GraphMessage] = Set()
    messageSet.foreach { message =>
      Source.single(message).via(validator).runWith(Sink.foreach {
        case Left(_) =>
          failedGraphMessages += message
        case _ => /* Nothing to do */
      })
    }

    if (failedGraphMessages.nonEmpty) {
      println(s"Error in the pipe ($remainingAttempts left): " + "\nOn msg: " + failedGraphMessages)
      passThroughPipeline(validator, failedGraphMessages, remainingAttempts - 1)
    }
  }
}
