package ch.epfl.pop.model.network.method.message.data.lao

import ch.epfl.pop.json.MessageDataProtocol.GreetLaoFormat
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.{Hash, PublicKey}
import spray.json._

final case class GreetLao(
    lao: Hash,
    frontend: PublicKey,
    address: String,
    peers: List[String]
) extends MessageData {
  override val _object: ObjectType = ObjectType.LAO
  override val action: ActionType = ActionType.GREET
}

object GreetLao extends Parsable {
  def apply(lao: Hash, frontend: PublicKey, address: String, peers: List[String]): GreetLao = {
    new GreetLao(lao, frontend, address, peers)
  }

  override def buildFromJson(payload: String): GreetLao = payload.parseJson.asJsObject.convertTo[GreetLao]
}
