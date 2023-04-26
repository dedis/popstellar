package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.actor.ActorRef
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Partition}
import ch.epfl.pop.model.network.JsonRpcResponse
import ch.epfl.pop.pubsub.graph.GraphMessage
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.storage.DbActor
import akka.stream.FlowShape
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.{Channel, DbActorNAckException}

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.util.Success

/** This object's job is to handle responses it receives from other servers after sending a heartbeat. When receiving the missing messages, the server's job is to write them on the data base.
  */
object GetMessagesByIdResponseHandler extends AskPatternConstants {

  private final val MAX_ATTEMPT: Int = 3

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
                    case Some(_) => portResponseHandler
                  }
              }
            case _ => portPipelineError // Pipeline error goes directly in handlerMerger
          }
        ))

        val responseHandler = builder.add(GetMessagesByIdResponseHandler.responseHandler(dbActorRef))
        val handlerMerger = builder.add(Merge[GraphMessage](totalPorts))

        /* glue the components together */
        handlerPartitioner.out(portPipelineError) ~> handlerMerger
        handlerPartitioner.out(portResponseHandler) ~> responseHandler ~> handlerMerger

        /* close the shape */
        FlowShape(handlerPartitioner.in, handlerMerger.out)
      }
  })

  private def responseHandler(dbActorRef: ActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcMessage: JsonRpcResponse) =>
      val receivedResponse = jsonRpcMessage.result.get.resultMap.get
      receivedResponse.keys.foreach(channel => {
        receivedResponse(channel).foreach(message => {
          writeOnDb(channel, message, dbActorRef, MAX_ATTEMPT)
        })
      })
      Right(jsonRpcMessage)
    case value @ _ => value
  }.filter(_ => false) // instead of implementing a Sink[GraphMessage], we chose to implement it as a Flow and filter every single outgoing graphMessage

 @tailrec
  private def writeOnDb(channel: Channel, message: Message, dbActorRef: AskableActorRef, remainingAttempts: Int): Unit = {
    if (remainingAttempts != 0) {
      val ask = dbActorRef ? DbActor.Write(channel, message)
      Await.ready(ask, duration).value match {
        case Some(Success(DbActorNAckException(_, _))) =>
          writeOnDb(channel, message, dbActorRef, remainingAttempts - 1)
        case _ =>
      }
    }
  }

}
