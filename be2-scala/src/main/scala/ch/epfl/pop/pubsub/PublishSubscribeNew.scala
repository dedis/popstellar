package ch.epfl.pop.pubsub

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Partition}
import ch.epfl.pop.pubsub.graph.handlers.ParamsWithMessageHandler
import ch.epfl.pop.pubsub.graph.{Answerer, GraphMessage, MessageDecoder, MessageEncoder, Validator}


// FIXME rename when old PublishSubscribe file is deleted
object PublishSubscribeNew extends App {

  def buildGraph(mediator: ActorRef)(implicit system: ActorSystem): Flow[Message, Message, NotUsed] = Flow.fromGraph(GraphDSL.create() {
    implicit builder: GraphDSL.Builder[NotUsed] => {
      import GraphDSL.Implicits._

      /* partitioner port numbers */
      val portPipelineError = 0
      val portParamsWithMessage = 1
      val portParams = 2
      val totalPorts = 3


      /* building blocks */
      // input message from the client
      val input = builder.add(Flow[Message].collect { case TextMessage.Strict(s) => s })

      // json-schema validator
      val schemaValidator = builder.add(Validator.schemaValidator)

      val jsonRpcDecoder = builder.add(MessageDecoder.jsonRpcParser)
      val jsonRpcContentValidator = builder.add(Validator.jsonRpcContentValidator)

      val methodPartitioner = builder.add(Partition[GraphMessage](totalPorts, {
        case Right(_) => portPipelineError // Pipeline error goes directly in merger
        case _ => portParamsWithMessage // Publish and Broadcast messages
        case _ => portParams // FIXME route correctly depending on message
      }))

      val hasMessagePartition = builder.add(ParamsWithMessageHandler.graph)
      // val noMessagePartition = ??? // other

      val merger = builder.add(Merge[GraphMessage](totalPorts))

      val jsonRpcEncoder = builder.add(MessageEncoder.serializer)
      val jsonRpcAnswerer = builder.add(Answerer.answerer(mediator))

      // output message (answer) for the client
      val output = builder.add(Flow[Message])


      /* glue the components together */
      input ~> schemaValidator ~> jsonRpcDecoder ~> jsonRpcContentValidator ~> methodPartitioner

      methodPartitioner.out(portPipelineError) ~> merger
      methodPartitioner.out(portParamsWithMessage) ~> hasMessagePartition ~> merger
      methodPartitioner.out(portParams) ~> merger // FIXME add no message partition

      merger ~> jsonRpcEncoder ~> jsonRpcAnswerer ~> output


      /* close the shape */
      FlowShape(input.in, output.out)
    }
  })
}
