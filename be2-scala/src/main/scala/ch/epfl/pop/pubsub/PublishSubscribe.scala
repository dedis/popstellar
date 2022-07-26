package ch.epfl.pop.pubsub

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.pattern.AskableActorRef
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Partition}
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.pubsub.graph._
import ch.epfl.pop.pubsub.graph.handlers.{ParamsHandler, ParamsWithMessageHandler}

object PublishSubscribe {

  private var dbActorRef: AskableActorRef = _

  def getDbActorRef: AskableActorRef = dbActorRef

  def buildGraph(mediatorActorRef: ActorRef, dbActorRefT: AskableActorRef, messageRegistry: MessageRegistry)(implicit system: ActorSystem): Flow[Message, Message, NotUsed] = Flow.fromGraph(GraphDSL.create() {
    implicit builder: GraphDSL.Builder[NotUsed] =>
      {
        import GraphDSL.Implicits._

        val clientActorRef: ActorRef = system.actorOf(ClientActor.props(mediatorActorRef))
        dbActorRef = dbActorRefT

        /* partitioner port numbers */
        val portPipelineError = 0
        val portParamsWithMessage = 1
        val portParams = 2
        val totalPorts = 3

        /* building blocks */
        // input message from the client
        val input = builder.add(Flow[Message].collect { case TextMessage.Strict(s) => println(s">>> Incoming message : $s"); s })

        val schemaVerifier = builder.add(SchemaVerifier.rpcSchemaVerifier)

        val jsonRpcDecoder = builder.add(MessageDecoder.jsonRpcParser)
        val jsonRpcContentValidator = builder.add(Validator.jsonRpcContentValidator)

        val methodPartitioner = builder.add(Partition[GraphMessage](
          totalPorts,
          {
            case Left(m: JsonRpcRequest) if m.hasParamsMessage => portParamsWithMessage // Publish and Broadcast messages
            case Left(_)                                       => portParams
            case _                                             => portPipelineError // Pipeline error goes directly in merger
          }
        ))

        val hasMessagePartition = builder.add(ParamsWithMessageHandler.graph(messageRegistry))
        val noMessagePartition = builder.add(ParamsHandler.graph(clientActorRef))

        val merger = builder.add(Merge[GraphMessage](totalPorts))

        val jsonRpcAnswerGenerator = builder.add(AnswerGenerator.generator)
        val jsonRpcAnswerer = builder.add(Answerer.answerer(clientActorRef, mediatorActorRef))

        // output message (answer) for the client
        val output = builder.add(Flow[Message])

        /* glue the components together */
        input ~> schemaVerifier ~> jsonRpcDecoder ~> jsonRpcContentValidator ~> methodPartitioner

        methodPartitioner.out(portPipelineError) ~> merger
        methodPartitioner.out(portParamsWithMessage) ~> hasMessagePartition ~> merger
        methodPartitioner.out(portParams) ~> noMessagePartition ~> merger

        merger ~> jsonRpcAnswerGenerator ~> jsonRpcAnswerer ~> output

        /* close the shape */
        FlowShape(input.in, output.out)
      }
  })
}
