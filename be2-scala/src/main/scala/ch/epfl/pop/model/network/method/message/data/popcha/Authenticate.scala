package ch.epfl.pop.model.network.method.message.data.popcha

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.{PublicKey, Signature}
import spray.json._

final case class Authenticate(
    clientId: String,
    nonce: String,
    identifier: PublicKey,
    identifierProof: Signature,
    state: String,
    responseMode: String,
    popchaAddress: String
) extends MessageData {
  override val _object: ObjectType = ObjectType.POPCHA
  override val action: ActionType = ActionType.AUTHENTICATE
}

object Authenticate extends Parsable {
  override def buildFromJson(payload: String): Authenticate = payload.parseJson.asJsObject.convertTo[Authenticate]
}
