package ch.epfl.pop

import ch.epfl.pop.json.Actions.Actions
import ch.epfl.pop.json.Objects.Objects



/** Collection of types used in Json parsing */
package object json {
  type UNKNOWN = String // TODO remove later

  type DecodedBase64String = Array[Byte]

  type TimeStamp = BigInt
  type Signature = DecodedBase64String
  type Key = DecodedBase64String
  type Hash = DecodedBase64String

  type ChannelName = String
  type ChannelMessage = MessageContent




  sealed trait Matching {
    // See https://stackoverflow.com/questions/3407032/comparing-string-and-enumeration
    def unapply(s: String): Boolean = s == toString
  }

  object Methods extends Enumeration {
    type Methods = Value

    val Subscribe: json.Methods.Value with Matching = MatchingValue("subscribe")
    val Unsubscribe: json.Methods.Value with Matching = MatchingValue("unsubscribe")
    val Message: json.Methods.Value with Matching = MatchingValue("message")
    val Catchup: json.Methods.Value with Matching = MatchingValue("catchup")
    val Publish: json.Methods.Value with Matching = MatchingValue("publish")

    def MatchingValue(v: String): Value with Matching = new Val(nextId, v) with Matching
    def unapply(s: String): Option[Value] = values.find(s == _.toString)
  }


  final case class MessageParameters(channel: ChannelName, message: Option[MessageContent])
  final case class MessageContent(
                                   data: MessageContentData, sender: Key, signature: Signature, message_id: Hash, witness_signatures: List[Key]
                                 )

  final case class ChannelMessages(messages: List[ChannelMessage])
  final case class MessageErrorContent(code: Int, description: String)

  /* --------------------------------------------------------- */
  /* ---------------------- ADMIN TYPES ---------------------- */
  /* --------------------------------------------------------- */

  object Objects extends Enumeration {
    type Objects = Value

    val Lao: json.Objects.Value with Matching = MatchingValue("lao")
    val Message: json.Objects.Value with Matching = MatchingValue("message")
    val Meeting: json.Objects.Value with Matching = MatchingValue("meeting")

    def MatchingValue(v: String): Value with Matching = new Val(nextId, v) with Matching
    def unapply(s: String): Option[Value] = values.find(s == _.toString)
  }


  object Actions extends Enumeration {
    type Actions = Value

    val Create: json.Actions.Value = MatchingValue("create")
    val UpdateProperties: json.Actions.Value = MatchingValue("update_properties")
    val State: json.Actions.Value = MatchingValue("state")
    val Witness: json.Actions.Value = MatchingValue("witness")

    def MatchingValue(v: String): Value with Matching =  new Val(nextId, v) with Matching
    def unapply(s: String): Option[Value] = values.find(s == _.toString)
  }


  /** Data field of a JSON message */
  final case class MessageContentData(
    /* basic common fields */
    _object: Objects,
    action: Actions,

    /* LAO related fields */
    id: Array[Byte],
    name: String,
    creation: TimeStamp,
    last_modified: TimeStamp,
    organizer: Key,
    witnesses: List[Key],

    /* witness a message related fields */
    message_id: Array[Byte],
    signature: Signature,

    /* meeting related fields */
    location: String,
    start: TimeStamp,
    end: TimeStamp,
    extra: UNKNOWN,
  )
}
