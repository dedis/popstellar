package ch.epfl.pop.model.network.method.message.data.coin

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects.{Hash, Transaction}
import spray.json._

final case class PostTransaction(
  transaction: Transaction,
  transactionId: Hash,
  ) extends MessageData {
    override val _object: ObjectType = ObjectType.COIN
    override val action: ActionType = ActionType.POST_TRANSACTION
}

object PostTransaction extends Parsable {
  override def buildFromJson(payload: String): PostTransaction = payload.parseJson.asJsObject.convertTo[PostTransaction]
}
