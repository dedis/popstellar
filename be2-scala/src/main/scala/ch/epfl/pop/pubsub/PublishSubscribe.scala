package ch.epfl.pop.pubsub

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.pattern.AskableActorRef
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Partition}
import ch.epfl.pop.decentralized.Monitor
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.pubsub.graph._
import ch.epfl.pop.pubsub.graph.handlers.{ParamsWithChannelHandler, ParamsWithMessageHandler}

object PublishSubscribe {

  private var dbActorRef: AskableActorRef = _

  def getDbActorRef: AskableActorRef = dbActorRef

  def buildGraph(
      mediatorActorRef: ActorRef,
      dbActorRefT: AskableActorRef,
      messageRegistry: MessageRegistry,
      monitorRef: ActorRef,
      connectionMediatorRef: ActorRef,
      isServer: Boolean
  )(implicit system: ActorSystem): Flow[Message, Message, NotUsed] = Flow.fromGraph(GraphDSL.create() {
    implicit builder: GraphDSL.Builder[NotUsed] =>
      {
        import GraphDSL.Implicits._

        val clientActorRef: ActorRef = system.actorOf(ClientActor.props(mediatorActorRef, connectionMediatorRef, isServer))
        dbActorRef = dbActorRefT

        /* partitioner port numbers */
        val portPipelineError = 0
        val portParamsWithMessage = 1
        val portParamsWithChannel = 2
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
            case Right(m: JsonRpcRequest) if m.hasParamsMessage => portParamsWithMessage // Publish and Broadcast messages
            case Right(m: JsonRpcRequest) if m.hasParamsChannel => portParamsWithChannel
            case _                                              => portPipelineError // Pipeline error goes directly in merger
          }
        ))

        val hasMessagePartition = builder.add(ParamsWithMessageHandler.graph(messageRegistry))
        val hasChannelPartition = builder.add(ParamsWithChannelHandler.graph(clientActorRef))

        val merger = builder.add(Merge[GraphMessage](totalPorts))
        val broadcast = builder.add(Broadcast[GraphMessage](2))

        val monitorSink = builder.add(Monitor.sink(monitorRef))
        val jsonRpcAnswerGenerator = builder.add(AnswerGenerator.generator)
        val jsonRpcAnswerer = builder.add(Answerer.answerer(clientActorRef, mediatorActorRef))

        // output message (answer) for the client
        val output = builder.add(Flow[Message])

        /* glue the components together */
        input ~> schemaVerifier ~> jsonRpcDecoder ~> jsonRpcContentValidator ~> methodPartitioner

        methodPartitioner.out(portPipelineError) ~> merger
        methodPartitioner.out(portParamsWithMessage) ~> hasMessagePartition ~> merger
        methodPartitioner.out(portParamsWithChannel) ~> hasChannelPartition ~> merger

        merger ~> broadcast
        broadcast ~> jsonRpcAnswerGenerator ~> jsonRpcAnswerer ~> output
        broadcast ~> monitorSink

        /* close the shape */
        FlowShape(input.in, output.out)
      }
  })
}
