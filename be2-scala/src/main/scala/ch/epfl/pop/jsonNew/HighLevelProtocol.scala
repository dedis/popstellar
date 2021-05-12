package ch.epfl.pop.jsonNew

import ch.epfl.pop.model.network.{ErrorObject, JsonRpcRequest, JsonRpcResponse, MethodType}
import ch.epfl.pop.model.network.MethodType.MethodType
import ch.epfl.pop.model.network.method._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.{Base64Data, Hash, PublicKey, Signature, WitnessSignaturePair}
import ObjectProtocol._
import ch.epfl.pop.model.objects.Channel
import spray.json._

import scala.collection.immutable.ListMap

object HighLevelProtocol extends DefaultJsonProtocol {

  // ----------------------------------- ENUM FORMATTERS ----------------------------------- //
  implicit object methodTypeFormat extends RootJsonFormat[MethodType] {
    override def read(json: JsValue): MethodType = json match {
      case JsString(method) => MethodType.unapply(method).getOrElse(MethodType.INVALID)
      case _ => throw new IllegalArgumentException(s"Can't parse json value $json to a MethodType")
    }

    override def write(obj: MethodType): JsValue = JsString(obj.toString)
  }


  // ----------------------------------- HIGH-LEVEL FORMAT ----------------------------------- //
  implicit object messageFormat extends RootJsonFormat[Message] {
    final private val PARAM_DATA: String = "data"
    final private val PARAM_SENDER: String = "sender"
    final private val PARAM_SIGNATURE: String = "signature"
    final private val PARAM_MESSAGE_ID: String = "message_id"
    final private val PARAM_WITNESS_SIG: String = "witness_signatures"

    override def read(json: JsValue): Message =
      json.asJsObject.getFields(PARAM_DATA, PARAM_SENDER, PARAM_SIGNATURE, PARAM_MESSAGE_ID, PARAM_WITNESS_SIG) match {
        case Seq(data@JsString(_), sender@JsString(_), signature@JsString(_), messageId@JsString(_), JsArray(witnessSig)) =>
          Message(
            data.convertTo[Base64Data],
            sender.convertTo[PublicKey],
            signature.convertTo[Signature],
            messageId.convertTo[Hash],
            witnessSig.map(_.convertTo[WitnessSignaturePair]).toList
          )
        case _ => throw new IllegalArgumentException(s"Can't parse json value $json to a Message object")
      }

    override def write(obj: Message): JsValue = JsObject(
      PARAM_DATA -> obj.data.toJson,
      PARAM_SENDER -> obj.sender.toJson,
      PARAM_SIGNATURE -> obj.signature.toJson,
      PARAM_MESSAGE_ID -> obj.message_id.toJson,
      PARAM_WITNESS_SIG -> obj.witness_signatures.toJson // FIXME does this work? Otherwise use "JsArray(obj.witness_signatures.map(_.toJson).toVector))"
    )
  }

  implicit object ParamsFormat extends RootJsonFormat[Params] {
    final private val PARAM_CHANNEL: String = "channel"
    final private val OPTIONAL_PARAM_MESSAGE: String = "message"

    override def read(json: JsValue): Params = json.asJsObject.getFields(PARAM_CHANNEL) match {
      case Seq(channel@JsString(_)) => json.asJsObject.getFields(OPTIONAL_PARAM_MESSAGE) match {
        case Seq(message@JsObject(_)) =>
          println("channel: " +channel)
          new ParamsWithMessage(channel.convertTo[Channel], message.convertTo[Message])
        case Seq(_) => throw new IllegalArgumentException(s"Unrecognizable message value in $json")
        case _ => new Params(channel.convertTo[Channel])
      }
      case _ => throw new IllegalArgumentException(s"Unrecognizable channel value in $json")
    }

    override def write(obj: Params): JsValue = {
      var jsObjectContent: ListMap[String, JsValue] = ListMap[String, JsValue](PARAM_CHANNEL -> obj.channel.toJson)

      obj match {
        case params: ParamsWithMessage => jsObjectContent += (OPTIONAL_PARAM_MESSAGE -> params.message.toJson)
        case _ =>
      }

      JsObject(jsObjectContent)
    }
  }

  implicit val broadcastFormat: JsonFormat[Broadcast] = jsonFormat2(Broadcast.apply)
  implicit val catchupFormat: JsonFormat[Catchup] = jsonFormat1(Catchup.apply)
  implicit val publishFormat: JsonFormat[Publish] = jsonFormat2(Publish.apply)
  implicit val subscribeFormat: JsonFormat[Subscribe] = jsonFormat1(Subscribe.apply)
  implicit val unsubscribeFormat: JsonFormat[Unsubscribe] = jsonFormat1(Unsubscribe.apply)

  implicit val errorObjectFormat: JsonFormat[ErrorObject] = jsonFormat2(ErrorObject.apply)

  implicit object jsonRpcRequestFormat extends RootJsonFormat[JsonRpcRequest] {
    final private val PARAM_JSON_RPC: String = "jsonrpc"
    final private val PARAM_METHOD: String = "method"
    final private val PARAM_PARAMS: String = "params"
    final private val PARAM_ID: String = "id"

    override def read(json: JsValue): JsonRpcRequest = json.asJsObject.getFields(PARAM_JSON_RPC, PARAM_METHOD, PARAM_PARAMS, PARAM_ID) match {
      case Seq(JsString(version), method@JsString(_), params@JsObject(_), optId) =>
        val id: Option[Int] = optId match {
          case JsNumber(id) => Some(id.toInt)
          case JsNull => None
          case _ => throw new IllegalArgumentException(s"Can't parse json value $optId to an id (number or null)")
        }
        JsonRpcRequest(
          version,
          method.convertTo[MethodType],
          params.convertTo[Params],
          id
        )
      case _ => throw new IllegalArgumentException(s"Can't parse json value $json to a JsonRpcRequest object")
    }

    override def write(obj: JsonRpcRequest): JsValue = {
      val optId: JsValue = obj.id match {
        case Some(idx) => idx.toJson
        case _ => JsNull
      }

      JsObject(
        PARAM_JSON_RPC -> obj.jsonrpc.toJson,
        PARAM_METHOD -> obj.method.toJson,
        PARAM_PARAMS -> obj.params.toJson,
        PARAM_ID -> optId
      )
    }
  }

  implicit object jsonRpcResponseFormat extends RootJsonFormat[JsonRpcResponse] {
    final private val PARAM_JSON_RPC: String = "jsonrpc"
    final private val PARAM_RESULT: String = "result"
    final private val PARAM_ERROR: String = "error"
    final private val PARAM_ID: String = "id"

    override def read(json: JsValue): JsonRpcResponse = ???

    override def write(obj: JsonRpcResponse): JsValue = ???
  }
}
