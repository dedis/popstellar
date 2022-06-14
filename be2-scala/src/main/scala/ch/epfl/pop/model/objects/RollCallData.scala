package ch.epfl.pop.model.objects

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.rollCall.{CloseRollCall, CreateRollCall, OpenRollCall, ReopenRollCall}
import spray.json._

final case class RollCallData(updateId: Hash, state: ActionType) {
  def toJsonString: String = {
    val that: RollCallData = this // tricks the compiler into inferring the right type
    that.toJson.toString
  }

  def updateWith(message: Message): RollCallData = {
    message.decodedData match {
      case Some(create: CreateRollCall) => RollCallData(create.id, create.action)
      case Some(open: OpenRollCall) => RollCallData(open.update_id, open.action)
      case Some(reopen: ReopenRollCall) => RollCallData(reopen.update_id, reopen.action)
      case Some(close: CloseRollCall) => RollCallData(close.update_id, close.action)
      case _ => this
    }
  }
}

object RollCallData extends Parsable {

  override def buildFromJson(payload: String): RollCallData = payload.parseJson.asJsObject.convertTo[RollCallData]

  def getName: String = "rollCallData"
}
