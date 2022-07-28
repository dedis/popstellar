package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL}
import ch.epfl.pop.pubsub.MessageRegistry
import ch.epfl.pop.pubsub.graph.{GraphMessage, Handler, MessageDecoder, Validator}

object ParamsWithMessageHandler {
  def graph(registry: MessageRegistry): Flow[GraphMessage, GraphMessage, NotUsed] = Flow.fromGraph(GraphDSL.create() {
    implicit builder: GraphDSL.Builder[NotUsed] =>
      {
        import GraphDSL.Implicits._

        /* building blocks */
        val messageDecoder = builder.add(MessageDecoder.dataParser(registry))
        val messageContentValidator = builder.add(Validator.messageContentValidator(registry))

        val handler = builder.add(Handler.handler(registry))

        /* glue the components together */
        messageDecoder ~> messageContentValidator ~> handler

        /* close the shape */
        FlowShape(messageDecoder.in, handler.out)
      }
  })
}
