package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.actor.ActorRef
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Partition, Sink}
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.pubsub.{AskPatternConstants, ClientActor, PubSubMediator}
import ch.epfl.pop.storage.DbActor
import akka.stream.FlowShape

/**
 * This object's job is to handle responses it receives from other servers after sending a heartbeat.
 * When receiving the missing messages, the server's job is to write them on the data base.
 */
object GetMessagesByIdResponseHandler extends AskPatternConstants {

  def graph(dbActorRef: ActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow.fromGraph(GraphDSL.create() {
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
                    case Some(map) => portResponseHandler
                    case _         => portPipelineError
                  }
                case _ => portPipelineError
              }
            case _ => portPipelineError // Pipeline error goes directly in handlerMerger
          }
        ))

        val responseHandler = builder.add(GetMessagesByIdResponseHandler.responseHandler(dbActorRef))
        val handlerMerger = builder.add(Merge[GraphMessage](totalPorts))

        /* glue the components together */
        handlerPartitioner.out(portPipelineError) ~> handlerMerger
        handlerPartitioner.out(portResponseHandler) ~> responseHandler

        /* close the shape */
        FlowShape(handlerPartitioner.in, handlerMerger.out)

      }
  })

  private def responseHandler(dbActorRef: ActorRef): Sink[GraphMessage, NotUsed] = Flow[GraphMessage].collect {
    case Right(jsonRpcMessage: JsonRpcResponse) =>
      val receivedResponse = jsonRpcMessage.result.get.resultMap.get
      receivedResponse.keys.foreach(channel => {
        receivedResponse(channel).foreach(message => {
          dbActorRef ! DbActor.Write(channel, message)
        })
      })
  }.to(Sink.ignore)

}
