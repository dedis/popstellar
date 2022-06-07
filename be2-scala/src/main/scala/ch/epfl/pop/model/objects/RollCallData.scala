package ch.epfl.pop.model.objects

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.rollCall.{CloseRollCall, CreateRollCall, OpenRollCall, ReopenRollCall}
import spray.json._

final case class RollCallData(update_id: Hash, state: ActionType) {
  def toJsonString: String = {
    val that: RollCallData = this // tricks the compiler into inferring the right type
    that.toJson.toString
  }

  def updateWith(message: Message): RollCallData = {
    message.decodedData.fold(this) {
      case create: CreateRollCall => RollCallData(create.id, create.action)
      case open: OpenRollCall => RollCallData(open.update_id, open.action)
      case reopen: ReopenRollCall => RollCallData(reopen.update_id, reopen.action)
      case close: CloseRollCall => RollCallData(close.update_id, close.action)
      case _ => this
    }
  }
}

object RollCallData extends Parsable {

  override def buildFromJson(payload: String): RollCallData = payload.parseJson.asJsObject.convertTo[RollCallData]

  def getName: String = "rollcallData"
}
