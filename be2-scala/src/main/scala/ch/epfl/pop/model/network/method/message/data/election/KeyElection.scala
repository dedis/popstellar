package ch.epfl.pop.model.network.method.message.data.election

import ch.epfl.pop.json.MessageDataProtocol.KeyElectionFormat
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.{Hash, PublicKey}
import spray.json._

final case class KeyElection(
                              loa: Hash,
                              election: Hash,
                              election_key: PublicKey
                              ) extends MessageData {
  override val _object: ObjectType = ObjectType.ELECTION
  override val action: ActionType = ActionType.KEY
}

object KeyElection extends Parsable {
  def apply(
             lao: Hash,
             election: Hash,
             election_key: PublicKey
           ): KeyElection = new KeyElection(lao, election, election_key)

  override def buildFromJson(payload: String): KeyElection = payload.parseJson.asJsObject.convertTo[KeyElection]
}



