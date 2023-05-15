package ch.epfl.pop.json

import ch.epfl.pop.json.HighLevelProtocol.GreetServerFormat
import ch.epfl.pop.json.MessageDataProtocol.{PARAM_ACTION, PARAM_OBJECT}
import ch.epfl.pop.json.ObjectProtocol._
import ch.epfl.pop.model.network.MethodType.MethodType
import ch.epfl.pop.model.network._
import ch.epfl.pop.model.network.method._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.lao.GreetLao
import ch.epfl.pop.model.objects._
import spray.json._

import scala.collection.immutable.{HashMap, ListMap}
import scala.collection.mutable

object HighLevelProtocol extends DefaultJsonProtocol {

  final private val PARAM_CHANNEL: String = "channel"

  // ----------------------------------- ENUM FORMATTERS ----------------------------------- //
  implicit object methodTypeFormat extends RootJsonFormat[MethodType] {
    override def read(json: JsValue): MethodType = json match {
      case JsString(method) => MethodType.unapply(method).getOrElse(MethodType.INVALID)
      case _                => throw new IllegalArgumentException(s"Can't parse json value $json to a MethodType")
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
        case Seq(data @ JsString(_), sender @ JsString(_), signature @ JsString(_), messageId @ JsString(_), JsArray(witnessSig)) =>
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
      PARAM_WITNESS_SIG -> obj.witness_signatures.toJson
    )
  }

  implicit object ParamsWithChannelFormat extends RootJsonFormat[ParamsWithChannel] {
    final private val OPTIONAL_PARAM_MESSAGE: String = "message"

    override def read(json: JsValue): ParamsWithChannel = json.asJsObject.getFields(PARAM_CHANNEL) match {
      case Seq(channel @ JsString(_)) => json.asJsObject.getFields(OPTIONAL_PARAM_MESSAGE) match {
          case Seq(message @ JsObject(_)) => new ParamsWithMessage(channel.convertTo[Channel], message.convertTo[Message])
          case Seq(_)                     => throw new IllegalArgumentException(s"Unrecognizable '$OPTIONAL_PARAM_MESSAGE' value in $json")
          case _                          => new ParamsWithChannel(channel.convertTo[Channel])
        }
      case _ => throw new IllegalArgumentException(s"Unrecognizable '$PARAM_CHANNEL' value in $json")
    }

    override def write(obj: ParamsWithChannel): JsValue = {
      var jsObjectContent: ListMap[String, JsValue] = ListMap[String, JsValue](PARAM_CHANNEL -> obj.channel.toJson)

      obj match {
        case params: ParamsWithMessage => jsObjectContent += (OPTIONAL_PARAM_MESSAGE -> params.message.toJson)
        case _                         =>
      }

      JsObject(jsObjectContent)
    }
  }

  implicit object ParamsWithMapFormat extends RootJsonFormat[ParamsWithMap] {

    override def read(json: JsValue): ParamsWithMap = {
      val map = mutable.HashMap[Channel, Set[Hash]]()
      json.asJsObject.fields.foreach {
        case (k: String, JsArray(v)) => map.put(Channel(k), v.map(_.convertTo[Hash]).toSet)
        case _                       => throw new IllegalArgumentException(s"Unrecognizable value in $json")
      }

      new ParamsWithMap(HashMap.from(map))
    }

    override def write(obj: ParamsWithMap): JsValue = {
      val map = mutable.HashMap[String, JsValue]()
      obj.channelsToMessageIds.foreach {
        case (channel, set) =>
          map.put(channel.channel, set.toJson)
      }

      JsObject.apply(HashMap.from(map))
    }
  }

  implicit object ParamsFormat extends RootJsonFormat[Params] {

    override def read(json: JsValue): Params =
      json.asJsObject.getFields(PARAM_CHANNEL) match {
        case Seq(_) => ParamsWithChannelFormat.read(json)
        // if no Seq can be retrieved, the json is of type ParamsWithMap or faulty
        case _ => ParamsWithMapFormat.read(json)
      }

    override def write(obj: Params): JsValue =
      obj match {
        case paramsWithChannel: ParamsWithChannel => paramsWithChannel.toJson
        case paramsWithMap: ParamsWithMap         => paramsWithMap.toJson
      }

  }

  implicit object BroadcastFormat extends RootJsonFormat[Broadcast] {
    override def read(json: JsValue): Broadcast = {
      val params: ParamsWithMessage = json.convertTo[ParamsWithChannel].asInstanceOf[ParamsWithMessage]
      Broadcast(params.channel, params.message)
    }

    override def write(obj: Broadcast): JsValue = obj.toJson(ParamsFormat.write)
  }

  implicit object CatchupFormat extends RootJsonFormat[Catchup] {
    override def read(json: JsValue): Catchup = Catchup(json.convertTo[ParamsWithChannel].channel)

    override def write(obj: Catchup): JsValue = obj.toJson(ParamsFormat.write)
  }

  implicit object PublishFormat extends RootJsonFormat[Publish] {
    override def read(json: JsValue): Publish = {
      val params: ParamsWithMessage = json.convertTo[ParamsWithChannel].asInstanceOf[ParamsWithMessage]
      Publish(params.channel, params.message)
    }

    override def write(obj: Publish): JsValue = obj.toJson(ParamsFormat.write)
  }

  implicit object SubscribeFormat extends RootJsonFormat[Subscribe] {
    override def read(json: JsValue): Subscribe = Subscribe(json.convertTo[ParamsWithChannel].channel)

    override def write(obj: Subscribe): JsValue = obj.toJson(ParamsFormat.write)
  }

  implicit object UnsubscribeFormat extends RootJsonFormat[Unsubscribe] {
    override def read(json: JsValue): Unsubscribe = Unsubscribe(json.convertTo[ParamsWithChannel].channel)

    override def write(obj: Unsubscribe): JsValue = obj.toJson(ParamsFormat.write)
  }

  implicit object HeartbeatFormat extends RootJsonFormat[Heartbeat] {
    override def read(json: JsValue): Heartbeat =
      Heartbeat(json.convertTo[ParamsWithMap].channelsToMessageIds)

    override def write(obj: Heartbeat): JsValue = obj.toJson(ParamsWithMapFormat.write)
  }

  implicit object GetMessagesByIdFormat extends RootJsonFormat[GetMessagesById] {
    override def read(json: JsValue): GetMessagesById =
      GetMessagesById(json.convertTo[ParamsWithMap].channelsToMessageIds)

    override def write(obj: GetMessagesById): JsValue = obj.toJson(ParamsWithMapFormat.write)
  }

  implicit object GreetServerFormat extends RootJsonFormat[GreetServer] {
    final private val PARAM_PUBLIC_KEY : String = "public_key"
    final private val PARAM_CLIENT_ADDRESS: String = "client_address"
    final private val PARAM_SERVER_ADDRESS: String = "server_address"

    override def read(json: JsValue): GreetServer = {
      val jsonObject: JsObject = json.asJsObject
      jsonObject.getFields(PARAM_PUBLIC_KEY,PARAM_CLIENT_ADDRESS,PARAM_SERVER_ADDRESS) match {
        case Seq(publicKey@JsString(_), clientAddress@JsString(_), serverAddress@JsString(_)) =>
          GreetServer(
            publicKey.convertTo[PublicKey],
            clientAddress.convertTo[String],
            serverAddress.convertTo[String]
          )
        case _ => throw new IllegalArgumentException(s"Can't parse json value $json to a GreetServer object")
      }
    }

    override def write(obj: GreetServer): JsValue = {
      val jsObjectContent: ListMap[String, JsValue] = ListMap[String, JsValue](
        PARAM_PUBLIC_KEY -> obj.publicKey.toJson,
        PARAM_CLIENT_ADDRESS -> obj.clientAddress.toJson,
        PARAM_SERVER_ADDRESS -> obj.clientAddress.toJson
      )
      JsObject(jsObjectContent)
    }
  }




  implicit val errorObjectFormat: JsonFormat[ErrorObject] = jsonFormat2(ErrorObject.apply)

  implicit object ResultObjectFormat extends RootJsonFormat[ResultObject] {
    override def read(json: JsValue): ResultObject = json match {
      case JsNumber(resultInt)  => new ResultObject(resultInt.toInt)
      case JsArray(resultArray) => new ResultObject(resultArray.map(_.convertTo[Message]).toList)
      case JsObject(resultMap)  => new ResultObject(resultMap.map { case (k, v) => (Channel(k), v.convertTo[Set[Message]]) })
      case _                    => throw new IllegalArgumentException(s"Unrecognizable channel value in $json")
    }

    override def write(obj: ResultObject): JsValue = {
      if (obj.isIntResult) {
        JsNumber(obj.resultInt.getOrElse(0))
      } else if (obj.resultMap.isDefined) {
        JsObject(obj.resultMap.get.map { case (chan, set) => (chan.channel, set.toJson) })
      } else {
        JsArray(obj.resultMessages.getOrElse(Nil).map(m => m.toJson).toVector)
      }
    }
  }

  implicit object jsonRpcRequestFormat extends RootJsonFormat[JsonRpcRequest] {
    final private val PARAM_JSON_RPC: String = "jsonrpc"
    final private val PARAM_METHOD: String = "method"
    final private val PARAM_PARAMS: String = "params"
    final private val PARAM_ID: String = "id"

    override def read(json: JsValue): JsonRpcRequest = json.asJsObject.getFields(PARAM_JSON_RPC, PARAM_METHOD, PARAM_PARAMS, PARAM_ID) match {
      case Seq(JsString(version), methodJsString @ JsString(_), paramsJsObject @ JsObject(_), optId) =>
        val method: MethodType = methodJsString.convertTo[MethodType]
        val params = method match {
          case MethodType.PUBLISH            => paramsJsObject.convertTo[Publish]
          case MethodType.SUBSCRIBE          => paramsJsObject.convertTo[Subscribe]
          case MethodType.UNSUBSCRIBE        => paramsJsObject.convertTo[Unsubscribe]
          case MethodType.CATCHUP            => paramsJsObject.convertTo[Catchup]
          case MethodType.GET_MESSAGES_BY_ID => paramsJsObject.convertTo[GetMessagesById]
          case _                             => throw new IllegalArgumentException(s"Can't parse json value $json with unknown method ${method.toString}")
        }

        val id: Option[Int] = optId match {
          case JsNumber(id) => Some(id.toInt)
          case JsNull       => None
          case _            => throw new IllegalArgumentException(s"Can't parse json value $optId to an id (number or null)")
        }

        JsonRpcRequest(version, method, params, id)

      // Heartbeat and Broadcast do not have an Id and should be treated separately
      case Seq(JsString(version), methodJsString @ JsString(_), paramsJsObject @ JsObject(_)) =>
        val method: MethodType = methodJsString.convertTo[MethodType]
        val params = method match {
          case MethodType.HEARTBEAT => paramsJsObject.convertTo[Heartbeat]
          case MethodType.BROADCAST => paramsJsObject.convertTo[Broadcast]
          case MethodType.GREET_SERVER =>       paramsJsObject.convertTo[GreetServer]
          case _                    => throw new IllegalArgumentException(s"Can't parse json value $json with unknown method ${method.toString}")
        }
        JsonRpcRequest(version, method, params, None)
      case _ => throw new IllegalArgumentException(s"Can't parse json value $json to a JsonRpcRequest object")
    }

    override def write(obj: JsonRpcRequest): JsValue = {

      var jsObjectContent: ListMap[String, JsValue] = ListMap.empty[String, JsValue]

      jsObjectContent += (PARAM_JSON_RPC -> obj.jsonrpc.toJson)
      jsObjectContent += (PARAM_METHOD -> obj.method.toJson)
      jsObjectContent += (PARAM_PARAMS -> obj.params.toJson)

      /* Add the id key iff it's non null */
      obj.id.foreach(id => jsObjectContent += (PARAM_ID -> id.toJson))

      JsObject(jsObjectContent)

    }
  }

  implicit object jsonRpcResponseFormat extends RootJsonFormat[JsonRpcResponse] {
    final private val PARAM_JSON_RPC: String = "jsonrpc"
    final private val PARAM_RESULT: String = "result"
    final private val PARAM_ERROR: String = "error"
    final private val PARAM_ID: String = "id"

    override def read(json: JsValue): JsonRpcResponse = json.asJsObject.getFields(PARAM_JSON_RPC, PARAM_ID) match {
      case Seq(JsString(version), id) =>
        val idOpt: Option[Int] = id match {
          case JsNumber(idx) => Some(idx.toInt)
          case JsNull        => None
          case _             => throw new IllegalArgumentException(s"Unable to parse json value $id to an id (number or null)")
        }

        val resultOpt: Option[ResultObject] = json.asJsObject.getFields(PARAM_RESULT) match {
          case Seq(result) => Some(result.convertTo[ResultObject])
          case _           => None
        }
        val errorOpt: Option[ErrorObject] = json.asJsObject.getFields(PARAM_ERROR) match {
          case Seq(error) => Some(error.convertTo[ErrorObject])
          case _          => None
        }

        if (resultOpt.isEmpty && errorOpt.isEmpty) {
          throw new IllegalArgumentException(
            s"Unable to parse json answer $json to a JsonRpcResponse object: 'result' and 'error' fields are missing or wrongly formatted"
          )
        } else {
          JsonRpcResponse(version, resultOpt, errorOpt, idOpt)
        }

      case _ => throw new IllegalArgumentException(
          s"Unable to parse json answer $json to a JsonRpcResponse object: 'jsonrpc' or 'id' field missing or wrongly formatted"
        )
    }

    override def write(obj: JsonRpcResponse): JsValue = {
      var jsObjectContent: ListMap[String, JsValue] = ListMap[String, JsValue](PARAM_JSON_RPC -> obj.jsonrpc.toJson)

      obj.id match {
        case Some(idx) => jsObjectContent += (PARAM_ID -> idx.toJson)
        case _         => jsObjectContent += (PARAM_ID -> JsNull)
      }

      // Adding either the result value of the error value depending on which is defined
      obj.result.foreach { r => jsObjectContent += (PARAM_RESULT -> r.toJson) }
      obj.error.foreach { e => jsObjectContent += (PARAM_ERROR -> e.toJson) }

      JsObject(jsObjectContent)
    }
  }

}
