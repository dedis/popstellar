package ch.epfl.pop.pubsub

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.SinkShape
import akka.stream.scaladsl.{GraphDSL, Merge, Partition, Sink}
import ch.epfl.pop.pubsub.graph.handlers.ParamsWithMessageHandler
import ch.epfl.pop.pubsub.graph.{Answerer, GraphMessage, MessageDecoder, MessageEncoder, Validator}

// FIXME rename when old PublishSubscribe file is deleted
object PublishSubscribeNew extends App {

  implicit val system: ActorSystem = ActorSystem() // typed actors? // FIXME actor system

  val graph = Sink.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._

    /* partitioner port numbers */
    val portPipelineError = 0
    val portParamsWithMessage = 1
    val portParams = 2
    val totalPorts = 3


    /* building blocks */
    // json-schema validation
    val schemaValidator = builder.add(Validator.schemaValidator)

    val jsonRpcDecoder = builder.add(MessageDecoder.jsonRpcParser)
    val jsonRpcContentValidator = builder.add(Validator.jsonRpcContentValidator)

    val methodPartitioner = builder.add(Partition[GraphMessage](totalPorts, {
      case Right(_) => portPipelineError // Pipeline error goes directly in merger
      case _ => portParamsWithMessage // Publish and Broadcast messages
      case _ => portParams // FIXME route correctly depending on message
    }))

    val hasMessagePartition = builder.add(ParamsWithMessageHandler.graph)
    val noMessagePartition = ??? // other

    val merger = builder.add(Merge[GraphMessage](totalPorts))

    val jsonRpcEncoder = builder.add(MessageEncoder.serializer)
    val jsonRpcAnswerer = builder.add(Answerer.answerer)


    /* glue the components together */
    schemaValidator ~> jsonRpcDecoder ~> jsonRpcContentValidator ~> methodPartitioner

    methodPartitioner.out(portPipelineError) ~> merger
    methodPartitioner.out(portParamsWithMessage) ~> hasMessagePartition ~> merger
    methodPartitioner.out(portParams) ~> merger // FIXME add no message partition

    merger ~> jsonRpcEncoder ~> jsonRpcAnswerer


    /* close the shape */
    SinkShape(schemaValidator.in)
  })



  // FIXME REMOVE THOSE TESTS (+ object extends App)
  println("test")

  // val source: Source[Int, NotUsed] = Source(0 to 5)
  // val a = graph.runWith(source)
}
