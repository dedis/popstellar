package ch.epfl.pop.model.objects

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.rollCall.{CloseRollCall, CreateRollCall, OpenRollCall, ReopenRollCall}
import spray.json._

final case class RollcallData(update_id: Hash, state: ActionType) {
  def toJsonString: String = {
    val that: RollcallData = this // tricks the compiler into inferring the right type
    that.toJson.toString
  }

  def updateWith(message: Message): RollcallData = {
    message.decodedData.fold(this) {
      case create: CreateRollCall => RollcallData(create.id, create.action)
      case open: OpenRollCall => RollcallData(open.update_id, open.action)
      case reopen: ReopenRollCall => RollcallData(reopen.update_id, reopen.action)
      case close: CloseRollCall => RollcallData(close.update_id, close.action)
      case _ => this
    }
  }
}

object RollcallData extends Parsable {
  def apply(
             update_id: Hash,
             state: ActionType
           ): RollcallData =
    RollcallData(update_id, state)

  override def buildFromJson(payload: String): RollcallData = payload.parseJson.asJsObject.convertTo[RollcallData]

  def getName: String = "rollcallData"
}


