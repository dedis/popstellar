package ch.epfl.pop.pubsub.graph

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.jsonNew.HighLevelProtocol._
import ch.epfl.pop.jsonNew.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{DataBuilder, MessageData, ProtocolException}
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
        "MessageDecoder parsing failed : input json is not correctly formatted"
      ))
    }

    case Right(pipelineError) => Right(pipelineError) // implicit typecasting
  }

  /**
   * Graph component: takes a GraphMessage and parses the 'data' field of any JsonRpcRequest
   * query containing a message. The result is stored in the input JsonRpcRequest's Message's
   * decodedData field
   */
  val dataParser: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map(parseData)


  /**
   * Decodes the JsonRpcRequest's Message 'data' field
   *
   * @param _object object of the targeted MessageData
   * @param action action of the targeted MessageData
   * @return the decoded 'data' field as a subclass of MessageData
   */
  @throws(classOf[ProtocolException])
  private def parseMessageData(dataString: String, _object: ObjectType, action: ActionType): MessageData =
    DataBuilder.buildData(_object, action, dataString)


  /**
   * Decides whether a graph message 'data' field should be decoded or not.
   * If yes, the 'data' field is decoded
   *
   * @param graphMessage graph message that should be further decoded
   * @return the upgraded graph message (with 'data' field decoded) or an error
   */
  def parseData(graphMessage: GraphMessage): GraphMessage = graphMessage match {
    case Left(jsonRpcRequest: JsonRpcRequest) => jsonRpcRequest.getDecodedData match {
      case Some(_) => graphMessage // do nothing if 'data' already decoded
      case _ if !jsonRpcRequest.hasParamsMessage => graphMessage // do nothing if rpc-message doesn't contain any message
      case _ =>
        val jsonString: String = jsonRpcRequest.getEncodedData.get.decode()
        Try(jsonString.parseJson.asJsObject.getFields("object", "action")) match {
          case Success(Seq(objectString@JsString(_), actionString@JsString(_))) => Try {
            val messageData = parseMessageData(jsonString, objectString.convertTo[ObjectType], actionString.convertTo[ActionType])
            jsonRpcRequest.setDecodedData(messageData)
          } match {
            case Success(_) => graphMessage // everything worked at expected, 'decodedData' field was populated
            case Failure(exception) => Right(PipelineError(ErrorCodes.INVALID_DATA.id, s"Invalid data: $exception"))
          }
          case Success(_) => Right(PipelineError(
            ErrorCodes.INVALID_DATA.id,
            "Invalid data: Unable to parse 'data' field: 'object' or 'action' field is missing/wrongly formatted"
          ))
          case _ => Right(PipelineError(
            ErrorCodes.INVALID_DATA.id,
            "Invalid data: Unable to parse 'data' field: 'data' is not a valid json string"
          ))
        }
    }
    case graphMessage@_ => graphMessage
  }
}
