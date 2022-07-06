package ch.epfl.pop.model.network.method.message.data.election

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.election.VersionType.VersionType
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.{Hash, Timestamp}
import spray.json._

final case class SetupElection(
    id: Hash,
    loa: Hash,
    name: String,
    version: VersionType,
    created_at: Timestamp,
    start_time: Timestamp,
    end_time: Timestamp,
    questions: List[ElectionQuestion]
) extends MessageData {
  override val _object: ObjectType = ObjectType.ELECTION
  override val action: ActionType = ActionType.SETUP
}

object SetupElection extends Parsable {
  def apply(
      id: Hash,
      loa: Hash,
      name: String,
      version: VersionType,
      created_at: Timestamp,
      start_time: Timestamp,
      end_time: Timestamp,
      questions: List[ElectionQuestion]
  ): SetupElection = new SetupElection(id, loa, name, version, created_at, start_time, end_time, questions)

  override def buildFromJson(payload: String): SetupElection = payload.parseJson.asJsObject.convertTo[SetupElection]
}
