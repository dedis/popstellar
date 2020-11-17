package ch.epfl.pop

import ch.epfl.pop.json.Actions.Actions
import ch.epfl.pop.json.Objects.Objects



/** Collection of types used in JsonMessage */
package object JsonMessageTypes {
  type UNKNOWN = String // TODO remove later

  type TimeStamp = BigInt
  type Signature = String
  type Key = String
  type Hash = String

  // OLD
  type ChannelName = String






  type MessageMethod = String // TODO ENUM : publish
  type MessageError = String // TODO




  final case class MessageParameters(channel: String, message: MessageContent)
  final case class MessageContent(
    data: MessageContentData, sender: Key, signature: Signature, message_id: Hash, witness_signatures: List[Key]
  )




  object Objects extends Enumeration {
    // See https://stackoverflow.com/questions/3407032/comparing-string-and-enumeration
    type Objects = Value

    val Lao: json.Objects.Value with Matching = MatchingValue("lao")
    val Message: json.Objects.Value with Matching = MatchingValue("message")
    val Meeting: json.Objects.Value with Matching = MatchingValue("meeting")

    sealed trait Matching {
      def unapply(s: String): Boolean = s == toString
    }

    def MatchingValue(v: String): Value with Matching = new Val(nextId, v) with Matching
    def unapply(s: String): Option[Value] = values.find(s == _.toString)
  }

  object Actions extends Enumeration {
    // See https://stackoverflow.com/questions/3407032/comparing-string-and-enumeration
    type Actions = Value

    val Create: json.Actions.Value = MatchingValue("create")
    val UpdateProperties: json.Actions.Value = MatchingValue("update_properties")
    val State: json.Actions.Value = MatchingValue("state")
    val Witness: json.Actions.Value = MatchingValue("witness")

    sealed trait Matching {
      def unapply(s: String): Boolean = s == toString
    }

    def MatchingValue(v: String): Value with Matching =  new Val(nextId, v) with Matching
    def unapply(s: String): Option[Value] = values.find(s == _.toString)
  }



  final case class MessageContentData(
    /* basic common fields */
    _object: Objects,
    action: Actions,

    /* LAO related fields */
    id: Int,
    name: String,
    creation: TimeStamp,
    last_modified: TimeStamp,
    organizer: Key,
    witnesses: List[Key],

    /* witness a message related fields */
    message_id: Int,
    signature: Signature,

    /* meeting related fields */
    location: UNKNOWN,
    start: TimeStamp,
    end: TimeStamp,
    extra: UNKNOWN,
  )


  final class MessageContentDataBuilder() {
    /* basic common fields */
    var _object: Objects = _
    var action: Actions = _

    /* LAO related fields */
    var id: Int = -1
    var name: String = ""
    var creation: TimeStamp = BigInt(-1)
    var last_modified: TimeStamp = BigInt(-1)
    var organizer: Key = ""
    var witnesses: List[Key] = List()

    /* witness a message related fields */
    var message_id: Int = -1
    var signature: Signature = ""

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
    def setId(id: Int): MessageContentDataBuilder = { this.id = id; this }
    def setName(name: String): MessageContentDataBuilder = { this.name = name; this }
    def setCreation(creation: TimeStamp): MessageContentDataBuilder = { this.creation = creation; this }
    def setLastModified(lastModified: TimeStamp): MessageContentDataBuilder = { this.last_modified = lastModified; this }
    def setOrganizer(organizer: Key): MessageContentDataBuilder = { this.organizer = organizer; this }
    def setWitnesses(witnesses: List[Key]): MessageContentDataBuilder = { this.witnesses = witnesses; this }
    def setMessageId(id: Int): MessageContentDataBuilder = { this.message_id = id; this }
    def setSignature(signature: Signature): MessageContentDataBuilder = { this.signature = signature; this }
    def setLocation(location: UNKNOWN): MessageContentDataBuilder = { this.location = location; this }
    def setStart(start: TimeStamp): MessageContentDataBuilder = { this.start = start; this }
    def setEnd(end: TimeStamp): MessageContentDataBuilder = { this.end = end; this }
    def setExtra(extra: UNKNOWN): MessageContentDataBuilder = { this.extra = extra; this }
  }
}
