package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Partition}
import ch.epfl.pop.pubsub.graph.{GraphMessage, MessageDecoder, Validator}

object ParamsWithMessageHandler {
  val graph: Flow[GraphMessage, GraphMessage, NotUsed] = Flow.fromGraph(GraphDSL.create() {
    implicit builder: GraphDSL.Builder[NotUsed] => {
      import GraphDSL.Implicits._

      /* partitioner port numbers */
      val portPipelineError = 0
      val portOther = 1
      val totalPorts = 2

      /* building blocks */
      val messageDecoder = builder.add(MessageDecoder.messageParser)
      val messageContentValidator = builder.add(Validator.messageContentValidator)

      val handlerPartitioner = builder.add(Partition[GraphMessage](totalPorts, {
        case Right(_) => portPipelineError // Pipeline error goes directly in handlerMerger
        case _ => portOther // FIXME rest of the messages
      }))

      val handlerMerger = builder.add(Merge[GraphMessage](totalPorts))

      /* glue the components together */
      messageDecoder ~> messageContentValidator ~> handlerPartitioner
      handlerPartitioner.out(portPipelineError) ~> handlerMerger
      handlerPartitioner.out(portOther) ~> handlerMerger // FIXME add sub-handlers

      /* close the shape */
      FlowShape(messageDecoder.in, handlerMerger.out)
    }
  })
}
