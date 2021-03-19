package ch.epfl.pop.pubsub.graph

import akka.stream.scaladsl.Flow

object Validator {

  type JsonString = String

  // takes a string (json) input and compares it with the JsonSchema.
  // /!\ Json Schema plugin?
  val schemaValidator = Flow[JsonString].map {
    val isCorrect: Boolean = true

    if (isCorrect) {
      // pass JsonString to parser
    } else {
      // raise an error message and send it to pubsub
    }

    ???
  }

  val jsonRpcContentValidator = ??? // takes a JsonRpcMessage and validates input until Message layer (see Validate.scala from archive)

  val messageContentValidator = ??? // validation from Message layer
}
