package ch.epfl.pop.pubsub.graph

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.jsonNew.HighLevelProtocol._
import spray.json._

import scala.util.{Success, Try}

object MessageDecoder {

  val jsonRpcParser: Flow[Either[JsonString, PipelineError], GraphMessage, NotUsed] = Flow[Either[JsonString, PipelineError]].map {
    case Left(jsonString) => Try(jsonString.parseJson.asJsObject) match {
      case Success(obj) =>
        val fields: Set[String] = obj.fields.keySet

        if (fields.contains("method")) { // FIXME check that the error (if any) is correctly propagated
          Left(obj.convertTo[JsonRpcRequest])
        } else {
          Left(obj.convertTo[JsonRpcResponse])
        }
      case _ => Right(PipelineError(
        ErrorCodes.INVALID_DATA.id,
        "MessageDecoder parsing failed : input json is not correctly formatted"
      ))
    }

    case Right(pipelineError) => Right(pipelineError) // implicit typecasting
  }

  val messageParser: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map(parseMessage)

  def parseMessage(graphMessage: GraphMessage): GraphMessage = graphMessage
}
