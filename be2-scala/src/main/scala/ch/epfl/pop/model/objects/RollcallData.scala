package ch.epfl.pop.model.objects

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.rollCall.{CloseRollCall, CreateRollCall, OpenRollCall, ReopenRollCall}
import spray.json._

final case class RollcallData(lao_id: Hash, update_id: Hash, state: ActionType) {
  def toJsonString: String = {
    val that: RollcallData = this // tricks the compiler into inferring the right type
    that.toJson.toString
  }

  def updateWith(message: Message): RollcallData = {
    message.decodedData.fold(this) {
      case create: CreateRollCall => RollcallData(lao_id, create.id, create.action)
      case open: OpenRollCall => RollcallData(lao_id, open.update_id, open.action)
      case reopen: ReopenRollCall => RollcallData(lao_id, reopen.update_id, reopen.action)
      case close: CloseRollCall => RollcallData(lao_id, close.update_id, close.action)
      case _ => this
    }
  }
}

object RollcallData extends Parsable {
  def apply(
             lao_id: Hash,
             update_id: Hash,
             state: ActionType
           ): RollcallData =
    RollcallData(lao_id, update_id, state)

  override def buildFromJson(payload: String): RollcallData = payload.parseJson.asJsObject.convertTo[RollcallData]

  def getName: String = "rollcallData"
}


