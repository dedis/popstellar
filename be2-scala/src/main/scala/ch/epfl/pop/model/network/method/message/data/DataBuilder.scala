package ch.epfl.pop.model.network.method.message.data

import ch.epfl.pop.model.network.method.message.data.lao.{CreateLao, StateLao, UpdateLao}
import ch.epfl.pop.model.network.method.message.data.meeting.{CreateMeeting, StateMeeting}
import ch.epfl.pop.model.network.method.message.data.rollCall.{CloseRollCall, CreateRollCall, OpenRollCall, ReopenRollCall}
import ch.epfl.pop.model.network.method.message.data.witness.WitnessMessage

object DataBuilder {
  def buildData(messageData: MessageData, payload: String): Any = messageData._object match {
    case ObjectType.LAO => buildLaoData(messageData, payload)
    case ObjectType.MEETING => buildMeetingData(messageData, payload)
    case ObjectType.ROLL_CALL => buildRollCallData(messageData, payload)
    case ObjectType.MESSAGE => buildWitnessData(messageData, payload)
    case _ => throw new ProtocolException(s"Unknown object '${messageData._object}' encountered while creating a Data")
  }

  def buildLaoData(messageData: MessageData, payload: String): Any = messageData.action match {
    case ActionType.CREATE => CreateLao.buildFromJson(messageData, payload)
    case ActionType.STATE => StateLao.buildFromJson(messageData, payload)
    case ActionType.UPDATE_PROPERTIES => UpdateLao.buildFromJson(messageData, payload)
    case _ => throw new ProtocolException(s"Unknown action '${messageData.action}' encountered while creating a Lao Data")
  }

  def buildMeetingData(messageData: MessageData, payload: String): Any = messageData.action match {
    case ActionType.CREATE => CreateMeeting.buildFromJson(messageData, payload)
    case ActionType.STATE => StateMeeting.buildFromJson(messageData, payload)
    case _ => throw new ProtocolException(s"Unknown action '${messageData.action}' encountered while creating a Meeting Data")
  }

  def buildRollCallData(messageData: MessageData, payload: String): Any = messageData.action match {
    case ActionType.CREATE => CreateRollCall.buildFromJson(messageData, payload)
    case ActionType.OPEN => OpenRollCall.buildFromJson(messageData, payload)
    case ActionType.REOPEN => ReopenRollCall.buildFromJson(messageData, payload)
    case ActionType.CLOSE => CloseRollCall.buildFromJson(messageData, payload)
    case _ => throw new ProtocolException(s"Unknown action '${messageData.action}' encountered while creating a RollCall Data")
  }

  def buildWitnessData(messageData: MessageData, payload: String): Any = messageData.action match {
    case ActionType.WITNESS => WitnessMessage.buildFromJson(messageData, payload)
    case _ => throw new ProtocolException(s"Unknown action '${messageData.action}' encountered while creating a Witness Data")
  }
}
