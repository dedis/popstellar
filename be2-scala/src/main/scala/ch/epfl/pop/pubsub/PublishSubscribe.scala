package ch.epfl.pop.pubsub

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.pattern.AskableActorRef
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Partition, Sink}
import ch.epfl.pop.decentralized.Monitor
import ch.epfl.pop.model.network.MethodType._
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.pubsub.graph._
import ch.epfl.pop.pubsub.graph.handlers.{GetMessagesByIdResponseHandler, ParamsHandler, ParamsWithMapHandler, ParamsWithMessageHandler}

object PublishSubscribe {

  private var dbActorRef: AskableActorRef = _

  def getDbActorRef: AskableActorRef = dbActorRef

  def buildGraph(
      mediatorActorRef: ActorRef,
      dbActorRefT: AskableActorRef,
      messageRegistry: MessageRegistry,
      monitorRef: ActorRef,
      connectionMediatorRef: ActorRef,
      isServer: Boolean,
      initGreetServer: Boolean = false
  )(implicit system: ActorSystem): Flow[Message, Message, NotUsed] = Flow.fromGraph(GraphDSL.create() {
    implicit builder: GraphDSL.Builder[NotUsed] =>
      {
        import GraphDSL.Implicits._

        val clientActorRef: ActorRef = system.actorOf(ClientActor.props(mediatorActorRef, connectionMediatorRef, isServer, initGreetServer))
        dbActorRef = dbActorRefT

        /* partitioner port numbers */
        val portPipelineError = 0
        val portRpcRequest = 1
        val portRpcResponse = 2
        val totalPorts = 3

        val totalBroadcastPort = 2

        /* building blocks */
        // input message from the client
        val input = builder.add(Flow[Message].collect { case TextMessage.Strict(s) => println(s">>> Incoming message : $s"); s })
        val schemaVerifier = builder.add(SchemaVerifier.rpcSchemaVerifier)
        val jsonRpcDecoder = builder.add(MessageDecoder.jsonRpcParser)

        val methodPartitioner = builder.add(Partition[GraphMessage](
          totalPorts,
          {
            case Right(_: JsonRpcRequest)  => portRpcRequest
            case Right(_: JsonRpcResponse) => portRpcResponse
            case _                         => portPipelineError // Pipeline error goes directly in merger
          }
        ))

        val requestPartition = builder.add(validateRequests(clientActorRef, messageRegistry))
        val responsePartition = builder.add(GetMessagesByIdResponseHandler.responseHandler(messageRegistry))

        // ResponseHandler messages do not go in the merger
        val merger = builder.add(Merge[GraphMessage](totalPorts - 1))
        val broadcast = builder.add(Broadcast[GraphMessage](totalBroadcastPort))

        val monitorSink = builder.add(Monitor.sink(monitorRef))
        val jsonRpcAnswerGenerator = builder.add(AnswerGenerator.generator)
        val jsonRpcAnswerer = builder.add(Answerer.answerer(clientActorRef, mediatorActorRef))

        // output message (answer) for the client
        val output = builder.add(Flow[Message])
        val droppingSink = builder.add(Sink.foreach(println))

        /* glue the components together */
        input ~> schemaVerifier ~> jsonRpcDecoder ~> methodPartitioner

        methodPartitioner.out(portPipelineError) ~> merger
        methodPartitioner.out(portRpcRequest) ~> requestPartition ~> merger
        methodPartitioner.out(portRpcResponse) ~> responsePartition ~> droppingSink

        merger ~> broadcast
        broadcast ~> jsonRpcAnswerGenerator ~> jsonRpcAnswerer ~> output
        broadcast ~> monitorSink

        /* close the shape */
        FlowShape(input.in, output.out)
      }
  })

  def validateRequests(clientActorRef: ActorRef, messageRegistry: MessageRegistry): Flow[GraphMessage, GraphMessage, NotUsed] =
    Flow.fromGraph(GraphDSL.create() {
      implicit builder: GraphDSL.Builder[NotUsed] =>
        {
          import GraphDSL.Implicits._

          /* partitioner port numbers */
          val portPipelineError = 0
          val portParamsWithMessage = 1
          val portSubscribe = 2
          val portUnsubscribe = 3
          val portCatchup = 4
          val portHeartbeat = 5
          val portGetMessagesById = 6
          val portGreetServer = 7
          val totalPorts = 8

          /* building blocks */
          val input = builder.add(Flow[GraphMessage].collect { case msg: GraphMessage => msg })
          val jsonRpcContentValidator = builder.add(Validator.jsonRpcContentValidator)

          val methodPartitioner = builder.add(Partition[GraphMessage](
            totalPorts,
            {
              case Right(m: JsonRpcRequest) => m.method match {
                  case BROADCAST          => portParamsWithMessage
                  case PUBLISH            => portParamsWithMessage
                  case SUBSCRIBE          => portSubscribe
                  case UNSUBSCRIBE        => portUnsubscribe
                  case CATCHUP            => portCatchup
                  case HEARTBEAT          => portHeartbeat
                  case GET_MESSAGES_BY_ID => portGetMessagesById
                  case GREET_SERVER       => portGreetServer
                  case _                  => portPipelineError
                }

              case _ => portPipelineError // Pipeline error goes directly in merger
            }
          ))

          val hasMessagePartition = builder.add(ParamsWithMessageHandler.graph(messageRegistry))
          val subscribePartition = builder.add(ParamsHandler.subscribeHandler(clientActorRef))
          val unsubscribePartition = builder.add(ParamsHandler.unsubscribeHandler(clientActorRef))
          val catchupPartition = builder.add(ParamsHandler.catchupHandler(clientActorRef))
          val heartbeatPartition = builder.add(ParamsWithMapHandler.heartbeatHandler(dbActorRef))
          val getMessagesByIdPartition = builder.add(ParamsWithMapHandler.getMessagesByIdHandler(dbActorRef))
          val greetServerPartition = builder.add(ParamsHandler.greetServerHandler(clientActorRef))

          val merger = builder.add(Merge[GraphMessage](totalPorts))

          /* glue the components together */
          input ~> jsonRpcContentValidator ~> methodPartitioner

          methodPartitioner.out(portPipelineError) ~> merger
          methodPartitioner.out(portParamsWithMessage) ~> hasMessagePartition ~> merger
          methodPartitioner.out(portSubscribe) ~> subscribePartition ~> merger
          methodPartitioner.out(portUnsubscribe) ~> unsubscribePartition ~> merger
          methodPartitioner.out(portCatchup) ~> catchupPartition ~> merger
          methodPartitioner.out(portHeartbeat) ~> heartbeatPartition ~> merger
          methodPartitioner.out(portGetMessagesById) ~> getMessagesByIdPartition ~> merger
          methodPartitioner.out(portGreetServer) ~> greetServerPartition ~> merger

          /* close the shape */
          FlowShape(input.in, merger.out)
        }
    })
}
