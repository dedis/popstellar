package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Partition, Sink, Source}
import ch.epfl.pop.model.network.method.Publish
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse, MethodType, ResultObject}
import ch.epfl.pop.model.objects.Channel
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError, Validator}
import ch.epfl.pop.pubsub.{AskPatternConstants, ClientActor, MessageRegistry}

import scala.annotation.tailrec
import scala.concurrent.Await

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

  // This function packs each message into a publish before pushing into the pipeline
  private def responseHandler(mediatorActorRef: ActorRef, messageRegistry: MessageRegistry)(implicit system: ActorSystem): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcMessage: JsonRpcResponse) =>
      val validatorFlow = validator(mediatorActorRef, messageRegistry)
      val receivedResponse: Map[Channel, Set[GraphMessage]] =
        jsonRpcMessage.result.get
          .resultMap.get
          .map {
            case (channel, set) => (channel, set.map(msg => Right(JsonRpcRequest(RpcValidator.JSON_RPC_VERSION, MethodType.PUBLISH, new Publish(channel, msg), Some(0)))))
          }

      val failedMessages = passThroughPipeline(receivedResponse, validatorFlow, MAX_RETRY)
      if (failedMessages.values.forall(set => set.isEmpty)) {
        Right(JsonRpcResponse(RpcValidator.JSON_RPC_VERSION, new ResultObject(0), None))
      } else {
        Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "GetMessagesById handler failed to process some messages", None))
      }

    // Will end up in a Sink.ignore, safe to let through
    case value @ _ => value
  }

  // This function will try to digest the set of messages for an entire channel
  // It will go channel by channel until it finished processing messages or MAX_RETRY is reached
  @tailrec
  private def passThroughPipeline(receivedResponse: Map[Channel, Set[GraphMessage]], validatorFlow: Flow[GraphMessage, GraphMessage, NotUsed], remainingAttempts: Int)(implicit system: ActorSystem): Map[Channel, Set[GraphMessage]] = {
    if (receivedResponse.isEmpty || remainingAttempts <= 0)
      return receivedResponse

    var failedMessages: Map[Channel, Set[GraphMessage]] = Map.empty
    receivedResponse.foreach {
      case (channel, messagesSet) =>
        val result = messageThroughPipeline(validatorFlow, messagesSet)
        failedMessages += channel -> result._1
        // only log a during last attempt
        if (remainingAttempts == 1 && result._2.nonEmpty) {
          println("Errors: " + result._2)
        }
    }

    passThroughPipeline(failedMessages, validatorFlow, remainingAttempts - 1)
  }

  // Push a single message through the pipeline
  private def messageThroughPipeline(validator: Flow[GraphMessage, GraphMessage, NotUsed], messageSet: Set[GraphMessage])(implicit system: ActorSystem): (Set[GraphMessage], List[PipelineError]) = {
    var failedGraphMessages: Set[GraphMessage] = Set()
    var errList: List[PipelineError] = Nil
    messageSet.foreach { message =>
      val singleRun = Source.single(message).via(validator).runWith(Sink.foreach {
        case Left(err: PipelineError) =>
          failedGraphMessages += message
          errList ::= err

        case _ => /* Nothing to do */
      })
      Await.ready(singleRun, duration)
    }

    (failedGraphMessages, errList)
  }

  // A partial graph to replay messages from get_message_by_id answers
  private def validator(mediatorActorRef: ActorRef, messageRegistry: MessageRegistry)(implicit system: ActorSystem): Flow[GraphMessage, GraphMessage, NotUsed] =
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

}
