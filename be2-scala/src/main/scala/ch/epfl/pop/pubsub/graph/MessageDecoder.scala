package ch.epfl.pop.pubsub.graph

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.json.HighLevelProtocol._
import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data._
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.pubsub.MessageRegistry
import spray.json._

import scala.util.{Failure, Success, Try}

object MessageDecoder {

  /**
   * Graph component: takes a string as input and parses it into a JsonRpcMessage
   * (stored into a GraphMessage)
   */
  val jsonRpcParser: Flow[Either[JsonString, PipelineError], GraphMessage, NotUsed] = Flow[Either[JsonString, PipelineError]].map {
    case Left(jsonString) => Try(jsonString.parseJson.asJsObject) match {
      case Success(obj) =>
        val fields: Set[String] = obj.fields.keySet

        if (fields.contains("method")) {
          Left(obj.convertTo[JsonRpcRequest])
        } else {
          Left(obj.convertTo[JsonRpcResponse])
        }
      case _ => Right(PipelineError(
        ErrorCodes.INVALID_DATA.id,
        "MessageDecoder parsing failed : input json is not correctly formatted (and thus unknown rpcId).",
        None // no rpcId since we couldn't decrypt the message
      ))
    }

    case Right(pipelineError) => Right(pipelineError) // implicit typecasting
  }

  /**
   * Graph component: takes a GraphMessage and parses the 'data' field of any JsonRpcRequest
   * query containing a message. The result is stored in the input JsonRpcRequest's Message's
   * decodedData field
   */
  def dataParser(registry: MessageRegistry): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map(parseData(_, registry))

  /**
   * Decides whether a graph message 'data' field should be decoded or not.
   * If yes, the 'data' field is decoded
   *
   * @param graphMessage graph message that should be further decoded
   * @return the upgraded graph message (with 'data' field decoded) or an error
   */
  def parseData(graphMessage: GraphMessage, registry: MessageRegistry): GraphMessage = graphMessage match {
    case Left(rpcRequest: JsonRpcRequest) => rpcRequest.getDecodedData match {
      case Some(_) =>
        println(f"Message was already decoded and of type $graphMessage")
        graphMessage // do nothing if 'data' already decoded
      case _ if !rpcRequest.hasParamsMessage => graphMessage // do nothing if rpc-message doesn't contain any message
      case _ =>
        // json string representation of the 'data' field
        val jsonString: String = rpcRequest.getEncodedData.get.decodeToString()

        // Try to extract data header from the json string
        Try(jsonString.parseJson.asJsObject.getFields("object", "action")) match {

          // if the header is correct (both 'object' and 'action' are present and both strings)
          case Success(Seq(objectString@JsString(_), actionString@JsString(_))) =>
            var filledRequest = rpcRequest // filler for the typed request

            Try {
              val _object: ObjectType = objectString.convertTo[ObjectType]
              val action: ActionType = actionString.convertTo[ActionType]

              // TODO [REFACTOR NICOLAS] : add data schema validation once refactored
              registry.getBuilder(_object, action) match {
                case Some(builder) =>
                  // decode the JsonRpcRequest's Message 'data' field
                  val messageData: MessageData = builder(jsonString)
                  rpcRequest.getWithDecodedData(messageData) match {
                    case Some(decodedJsonRequest) => filledRequest = decodedJsonRequest
                    // Should never be thrown since we check if the jsonRpcRequest hasParamMessage before parsing/decoding
                    case None => throw new IllegalStateException(s"JsonRpcRequest <$rpcRequest> does not contain a message data")
                  }
                case _ => throw new ProtocolException(s"MessageRegistry could not find any builder for JsonRpcRequest : $rpcRequest'")
              }

            } match {
              case Success(_) => Left(filledRequest) // everything worked at expected, 'decodedData' field was populated
              case Failure(exception) => Right(PipelineError(ErrorCodes.INVALID_DATA.id, s"Invalid data: ${exception.getMessage}", rpcRequest.id))
            }

          case Success(_) => Right(PipelineError(
            ErrorCodes.INVALID_DATA.id,
            "Invalid data: Unable to parse 'data' field: 'object' or 'action' field is missing/wrongly formatted",
            rpcRequest.id
          ))
          case _ => Right(PipelineError(
            ErrorCodes.INVALID_DATA.id,
            "Invalid data: Unable to parse 'data' field: 'data' is not a valid json string",
            rpcRequest.id
          ))
        }
    }
    case graphMessage@_ => graphMessage
  }
}
