package ch.epfl.pop.pubsub.graph

import akka.NotUsed
import akka.stream.scaladsl.Flow

object Validator {

  // FIXME implement schema
  def validateSchema(jsonString: JsonString): Either[JsonString, PipelineError] = Left(jsonString)

  // FIXME implement rpc validator
  def validateJsonRpcContent(graphMessage: GraphMessage): GraphMessage = graphMessage

  // FIXME implement message validator
  def validateMessageContent(graphMessage: GraphMessage): GraphMessage = graphMessage


  // takes a string (json) input and compares it with the JsonSchema
  // /!\ Json Schema plugin?
  val schemaValidator: Flow[JsonString, Either[JsonString, PipelineError], NotUsed] = Flow[JsonString].map(validateSchema)

  // takes a JsonRpcMessage and validates input until Message layer
  val jsonRpcContentValidator: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map(validateJsonRpcContent)

  // validation from Message layer
  val messageContentValidator: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map(validateMessageContent)
}
