package ch.epfl.pop.model.network.method.message.data.popcha

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.{PublicKey, Signature}
import spray.json._

/**
 * Data structure to represent an authentication request
 * @param clientId client to authenticate to
 * @param nonce nonce of the authentication request
 * @param identifier user's identity as the public key of the long term identifier
 * @param identifierProof proof of authentication
 * @param state state of the authentication request
 * @param responseMode response mode requested
 * @param popchaAddress address of the web socket to send the Id Token to
 */
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
