package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.actor.ActorRef
import akka.pattern.AskableActorRef
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Partition}
import ch.epfl.pop.model.network.method.{Catchup, GetMessagesById, Heartbeat, Subscribe, Unsubscribe}
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.model.objects.Channel
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.pubsub.{AskPatternConstants, ClientActor, PubSubMediator}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

object ParamsWithMapHandler extends AskPatternConstants{

  def graph(dbActorRef : AskableActorRef): Flow[GraphMessage, GraphMessage,NotUsed] = Flow.fromGraph(GraphDSL.create(){
    implicit builder: GraphDSL.Builder[NotUsed] =>
      {
        import GraphDSL.Implicits._

        /* partitioner port numbers */
        val portPipelineError = 0
        val portHeartBeatHandler = 1
        val portGetMsgsByIdHandler = 2
        val totalPorts = 3

        /* building blocks */
        val handlerPartitioner = builder.add(Partition[GraphMessage](
          totalPorts,
          {
            case Right(jsonRpcMessage: JsonRpcRequest) => jsonRpcMessage.getParams match {
              case _: Heartbeat => portHeartBeatHandler
              case _: GetMessagesById => portGetMsgsByIdHandler

            }
            case _ => portPipelineError // Pipeline error goes directly in handlerMerger
          }
        ))

        // ajouter les handlers

      }
  })



}
