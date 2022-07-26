package ch.epfl.pop.json

import ch.epfl.pop.model.objects._
import spray.json._

object ObjectProtocol extends DefaultJsonProtocol {

  implicit object AddressFormat extends JsonFormat[Address] {
    override def read(json: JsValue): Address = json match {
      case JsString(data) => Address(Base64Data(data))
      case _              => throw new IllegalArgumentException(s"Can't parse json value $json to an Address object")
    }

    override def write(obj: Address): JsValue = JsString(obj.base64Data.data)
  }

  implicit object Base64DataFormat extends JsonFormat[Base64Data] {
    override def read(json: JsValue): Base64Data = json match {
      case JsString(data) => Base64Data(data)
      case _              => throw new IllegalArgumentException(s"Can't parse json value $json to a Base64Data object")
    }

    override def write(obj: Base64Data): JsValue = JsString(obj.data)
  }

  implicit object ChannelFormat extends JsonFormat[Channel] {
    override def read(json: JsValue): Channel = json match {
      case JsString(value) => Channel(value)
      case _               => throw new IllegalArgumentException(s"Can't parse json value $json to a Channel object")
    }

    override def write(obj: Channel): JsValue = JsString(obj.channel)
  }

  implicit object HashFormat extends JsonFormat[Hash] {
    override def read(json: JsValue): Hash = json match {
      case dataJs @ JsString(_) => Hash(dataJs.convertTo[Base64Data])
      case _                    => throw new IllegalArgumentException(s"Can't parse json value $json to a Hash object")
    }

    override def write(obj: Hash): JsValue = obj.base64Data.toJson
  }

  implicit object PublicKeyFormat extends JsonFormat[PublicKey] {
    override def read(json: JsValue): PublicKey = json match {
      case dataJs @ JsString(_) => PublicKey(dataJs.convertTo[Base64Data])
      case _                    => throw new IllegalArgumentException(s"Can't parse json value $json to a PublicKey object")
    }

    override def write(obj: PublicKey): JsValue = obj.base64Data.toJson
  }

  implicit object PrivateKeyFormat extends JsonFormat[PrivateKey] {
    override def read(json: JsValue): PrivateKey = json match {
      case dataJs @ JsString(_) => PrivateKey(dataJs.convertTo[Base64Data])
      case _                    => throw new IllegalArgumentException(s"Can't parse json value $json to a PrivateKey object")
    }

    override def write(obj: PrivateKey): JsValue = obj.base64Data.toJson
  }

  implicit object SignatureFormat extends JsonFormat[Signature] {
    override def read(json: JsValue): Signature = json match {
      case dataJs @ JsString(_) => Signature(dataJs.convertTo[Base64Data])
      case _                    => throw new IllegalArgumentException(s"Can't parse json value $json to a Signature object")
    }

    override def write(obj: Signature): JsValue = obj.signature.toJson
  }

  implicit object TimestampFormat extends JsonFormat[Timestamp] {
    override def read(json: JsValue): Timestamp = json match {
      case JsNumber(time) => Timestamp(time.toLong)
      case _              => throw new IllegalArgumentException(s"Can't parse json value $json to a Timestamp object")
    }

    override def write(obj: Timestamp): JsValue = obj.time.toJson
  }

  implicit object WitnessSignaturePairFormat extends JsonFormat[WitnessSignaturePair] {
    final private val PARAM_WITNESS: String = "witness"
    final private val PARAM_SIGNATURE: String = "signature"

    override def read(json: JsValue): WitnessSignaturePair = json.asJsObject().getFields(PARAM_WITNESS, PARAM_SIGNATURE) match {
      case Seq(witness @ JsString(_), signature @ JsString(_)) => WitnessSignaturePair(
          witness.convertTo[PublicKey],
          signature.convertTo[Signature]
        )
      case _ => throw new IllegalArgumentException(s"Can't parse json value $json to a WitnessSignaturePair object")
    }

    override def write(obj: WitnessSignaturePair): JsValue = JsObject(
      PARAM_WITNESS -> JsString(obj.witness.base64Data.data),
      PARAM_SIGNATURE -> JsString(obj.signature.signature.data)
    )
  }

  implicit val lockScriptFormat: JsonFormat[LockScript] = jsonFormat[String, Address, LockScript](LockScript.apply, "type", "pubkey_hash")
  implicit val unlockScriptFormat: JsonFormat[UnlockScript] = jsonFormat[String, PublicKey, Base64Data, UnlockScript](UnlockScript.apply, "type", "pubkey", "sig")

  implicit val txInFormat: JsonFormat[Transaction.Input] = jsonFormat[Hash, Int, UnlockScript, Transaction.Input](Transaction.Input.apply, "tx_out_hash", "tx_out_index", "script")
  implicit val txOutFormat: JsonFormat[Transaction.Output] = jsonFormat[Long, LockScript, Transaction.Output](Transaction.Output.apply, "value", "script")

  implicit val transactionFormat: JsonFormat[Transaction] = jsonFormat[Int, List[Transaction.Input], List[Transaction.Output], Int, Transaction](Transaction.apply, "version", "inputs", "outputs", "lock_time")
}
