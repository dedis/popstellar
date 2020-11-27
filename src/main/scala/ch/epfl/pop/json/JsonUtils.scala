package ch.epfl.pop.json

import java.util.Base64

import ch.epfl.pop.json.Actions.Actions
import ch.epfl.pop.json.Objects.Objects


object JsonUtils {

  /** JSON-RPC protocol version */
  val JSON_RPC_VERSION: String = "2.0"

  val ENCODER: Base64.Encoder = Base64.getEncoder
  val DECODER: Base64.Decoder = Base64.getDecoder

  /*trait Base64Utils {
    val ENCODER: Base64.Encoder = Base64.getEncoder
    val DECODER: Base64.Decoder = Base64.getDecoder
  }*/


  /** Transform a String in hex ("0xabc") into a HexString ("abc") */
  /*def hexStringUnwrap(s: String): HexString = {
    if (s.startsWith("0x") || s.startsWith("0X")) s.drop(2)
    else s
  }

  /** Transform a HexString ("abc") into String in hex ("0xabc") */
  def hexStringWrap(s: HexString): String = {
    if (s.startsWith("0x")) s
    else "0x" + s
  }*/


  /** Builder for MessageContentData */
  final class MessageContentDataBuilder() {
    /* basic common fields */
    var _object: Objects = _
    var action: Actions = _

    /* LAO related fields */
    var id: DecodedBase64String = Array[Byte]()
    var name: String = ""
    var creation: TimeStamp = BigInt(-1)
    var last_modified: TimeStamp = BigInt(-1)
    var organizer: Key = Array[Byte]()
    var witnesses: List[Key] = List()

    /* witness a message related fields */
    var message_id: DecodedBase64String = Array[Byte]()
    var signature: Signature = Array[Byte]()

    /* meeting related fields */
    var location: String = ""
    var start: TimeStamp = BigInt(-1)
    var end: TimeStamp = BigInt(-1)
    var extra: UNKNOWN = ""


    def build(): MessageContentData = {
      if (_object == null || action == null) {
        println("Builder error! _object field or action field is null. Returning null")
        null
      } else {
        MessageContentData(
          _object, action,
          id, name, creation, last_modified, organizer, witnesses,
          message_id, signature,
          location, start, end, extra
        )
      }
    }

    def setHeader(obj: Objects, act: Actions): MessageContentDataBuilder = {setObject(obj); setAction(act); this }

    def setObject(obj: Objects): MessageContentDataBuilder = { this._object = obj; this }
    def setAction(action: Actions): MessageContentDataBuilder = { this.action = action; this }
    def setId(id: DecodedBase64String): MessageContentDataBuilder = { this.id = id; this }
    def setName(name: String): MessageContentDataBuilder = { this.name = name; this }
    def setCreation(creation: TimeStamp): MessageContentDataBuilder = { this.creation = creation; this }
    def setLastModified(lastModified: TimeStamp): MessageContentDataBuilder = { this.last_modified = lastModified; this }
    def setOrganizer(organizer: Key): MessageContentDataBuilder = { this.organizer = organizer; this }
    def setWitnesses(witnesses: List[Key]): MessageContentDataBuilder = { this.witnesses = witnesses; this }
    def setMessageId(id: DecodedBase64String): MessageContentDataBuilder = { this.message_id = id; this }
    def setSignature(signature: Signature): MessageContentDataBuilder = { this.signature = signature; this }
    def setLocation(location: String): MessageContentDataBuilder = { this.location = location; this }
    def setStart(start: TimeStamp): MessageContentDataBuilder = { this.start = start; this }
    def setEnd(end: TimeStamp): MessageContentDataBuilder = { this.end = end; this }
    def setExtra(extra: UNKNOWN): MessageContentDataBuilder = { this.extra = extra; this }
  }
}
