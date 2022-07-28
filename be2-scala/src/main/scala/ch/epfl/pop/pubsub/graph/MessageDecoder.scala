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

  /** Graph component: takes a string as input and parses it into a JsonRpcMessage (stored into a GraphMessage)
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

  /** Graph component: takes a GraphMessage and parses the 'data' field of any JsonRpcRequest query containing a message. The result is stored in the input JsonRpcRequest's Message's decodedData field
    */
  def dataParser(registry: MessageRegistry): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map(parseData(_, registry))

  private def populateDataField(rpcRequest: JsonRpcRequest, _object: ObjectType, action: ActionType, dataJsonString: String, registry: MessageRegistry): GraphMessage = {
    var filledRequest = rpcRequest // filler for the typed request

    Try {
      val validated: Try[Unit] = registry.getSchemaVerifier(_object, action) match {
        // validate the data schema if the registry found a mapping
        case Some(schemaVerifier) => schemaVerifier(dataJsonString)
        case _                    => throw new ProtocolException(s"MessageRegistry could not find any schema validator for JsonRpcRequest : $rpcRequest'")
      }

      val builder: JsonString => MessageData = validated match {
        // build the message if the registry found a mapping
        case Success(_) => registry.getBuilder(_object, action) match {
            case Some(b) => b
            case _       => throw new ProtocolException(s"MessageRegistry could not find any builder for JsonRpcRequest : $rpcRequest'")
          }
        case Failure(exception) => throw exception // propagate the exception
      }

      // decode the JsonRpcRequest's Message 'data' field
      val messageData: MessageData = builder(dataJsonString)
      rpcRequest.getWithDecodedData(messageData) match {
        case Some(decodedJsonRequest) => filledRequest = decodedJsonRequest
        // Should never be thrown since we check if the jsonRpcRequest hasParamMessage before parsing/decoding
        case _ => throw new IllegalStateException(s"JsonRpcRequest <$rpcRequest> does not contain a message data")
      }

    } match {
      case Success(_)         => Left(filledRequest) // everything worked at expected, 'decodedData' field was populated
      case Failure(exception) => Right(PipelineError(ErrorCodes.INVALID_DATA.id, s"Invalid data: ${exception.getMessage}", rpcRequest.id))
    }
  }

  /** Decides whether a graph message 'data' field should be decoded or not. If yes, the 'data' field is decoded
    *
    * @param graphMessage
    *   graph message that should be further decoded
    * @return
    *   the upgraded graph message (with 'data' field decoded) or an error
    */
  def parseData(graphMessage: GraphMessage, registry: MessageRegistry): GraphMessage = graphMessage match {
    case Left(rpcRequest: JsonRpcRequest) => rpcRequest.getDecodedData match {
        case Some(_) =>
          println(s"Message was already decoded and of type $graphMessage")
          graphMessage // do nothing if 'data' already decoded
        case _ if !rpcRequest.hasParamsMessage => graphMessage // do nothing if rpc-message doesn't contain any message
        case _                                 =>
          // json string representation of the 'data' field
          val jsonString: JsonString = rpcRequest.getEncodedData.fold("")(_.decodeToString())

          parseHeader(jsonString) match {
            case Success((_object, action)) =>
              populateDataField(rpcRequest, _object, action, jsonString, registry)
            case Failure(exception) =>
              Right(PipelineError(
                ErrorCodes.INVALID_DATA.id,
                s"Invalid header: ${exception.getMessage()}",
                rpcRequest.id
              ))
          }
      }
    case _ => graphMessage
  }
}
