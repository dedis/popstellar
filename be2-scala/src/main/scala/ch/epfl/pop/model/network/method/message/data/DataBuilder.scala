package ch.epfl.pop.model.network.method.message.data

import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.election.{CastVoteElection, EndElection, ResultElection, SetupElection}
import ch.epfl.pop.model.network.method.message.data.lao.{CreateLao, StateLao, UpdateLao}
import ch.epfl.pop.model.network.method.message.data.meeting.{CreateMeeting, StateMeeting}
import ch.epfl.pop.model.network.method.message.data.rollCall.{CloseRollCall, CreateRollCall, OpenRollCall, ReopenRollCall}
import ch.epfl.pop.model.network.method.message.data.witness.WitnessMessage
import ch.epfl.pop.model.network.method.message.data.socialMedia.{AddChirp, AddBroadcastChirp}
import spray.json._
import scala.util.{Try,Success,Failure}

/*
 * Helps building MessageData instances
 */
object DataBuilder {
  /**
   * Builds a MessageData from its headers ('object' and 'action' fields) and its json representation
   *
   * @param _object 'object' field of the message data
   * @param action  'action' field of the message data
   * @param payload json string representation of the message data
   * @throws ch.epfl.pop.model.network.method.message.data.ProtocolException if the tuple (_object, action) doesn't make sense with respect to the protocol
   * @return
   */
  @throws(classOf[ProtocolException])
  def buildData(_object: ObjectType, action: ActionType, payload: String): MessageData = _object match {
    case ObjectType.LAO => buildLaoData(action, payload)
    case ObjectType.MEETING => buildMeetingData(action, payload)
    case ObjectType.ROLL_CALL => buildRollCallData(action, payload)
    case ObjectType.ELECTION => buildElectionData(action, payload)
    case ObjectType.MESSAGE => buildWitnessData(action, payload)
    case ObjectType.CHIRP => buildSocialMediaData(action, payload)
    case _ => throw new ProtocolException(s"Unknown object '${_object}' encountered while creating a Data")
  }

  /**
    * builds MessageData from payload conformly to the protocol
    * after validating its schema
    * @param action action type
    * @param payload message data in json string format
    * @return parsed MessageData if the validation and build succedds, throws ProtocolException otherwise
    */
  @throws(classOf[ProtocolException])
  private def buildLaoData(action: ActionType, payload: String): MessageData = action match {
    case ActionType.CREATE =>
      buildOrReject(payload)(DataSchemaValidator.validateSchema(ObjectType.LAO)(ActionType.CREATE))(CreateLao.buildFromJson)("CreateLao data could not be parsed")
    case ActionType.STATE =>
      buildOrReject(payload)(DataSchemaValidator.validateSchema(ObjectType.LAO)(ActionType.STATE))(CreateLao.buildFromJson)("StateLao data could not be parsed")
    case ActionType.UPDATE_PROPERTIES =>
      buildOrReject(payload)(DataSchemaValidator.validateSchema(ObjectType.LAO)(ActionType.UPDATE_PROPERTIES))(CreateLao.buildFromJson)("UpdateLao data could not be parsed")
    case _ => throw new ProtocolException(s"Unknown action '$action' encountered while creating a Lao Data")
  }

  private def buildMeetingData(action: ActionType, payload: String): MessageData = action match {
    case ActionType.CREATE => CreateMeeting.buildFromJson(payload)
    case ActionType.STATE => StateMeeting.buildFromJson(payload)
    case _ => throw new ProtocolException(s"Unknown action '$action' encountered while creating a Meeting Data")
  }

  private def buildRollCallData(action: ActionType, payload: String): MessageData = action match {
    case ActionType.CREATE => CreateRollCall.buildFromJson(payload)
    case ActionType.OPEN => OpenRollCall.buildFromJson(payload)
    case ActionType.REOPEN => ReopenRollCall.buildFromJson(payload)
    case ActionType.CLOSE => CloseRollCall.buildFromJson(payload)
    case _ => throw new ProtocolException(s"Unknown action '$action' encountered while creating a RollCall Data")
  }

  private def buildElectionData(action: ActionType, payload: String): MessageData = action match {
    case ActionType.SETUP => SetupElection.buildFromJson(payload)
    case ActionType.RESULT => ResultElection.buildFromJson(payload)
    case ActionType.END => EndElection.buildFromJson(payload)
    case ActionType.CAST_VOTE => CastVoteElection.buildFromJson(payload)
    case _ => throw new ProtocolException(s"Unknown action '$action' encountered while creating a Election Data")
  }

  private def buildWitnessData(action: ActionType, payload: String): MessageData = action match {
    case ActionType.WITNESS => WitnessMessage.buildFromJson(payload)
    case _ => throw new ProtocolException(s"Unknown action '$action' encountered while creating a Witness Data")
  }

  private def buildSocialMediaData(action: ActionType, payload: String): MessageData = action match {
    case ActionType.ADD => AddChirp.buildFromJson(payload)
    case ActionType.ADD_BROADCAST => AddBroadcastChirp.buildFromJson(payload)
    case _ => throw new ProtocolException(s"Unknown action '$action' encountered while creating a Social Media Data")
  }
  /**
    * Builds a message payload after passing a schema validation check
    *
    * @param payload payload to build
    * @param validator one of the validators at [[DataSchemaValidator]] to valid the schema of the payload
    * @param buildFromJson the data builder
    * @param errMsg error message to include in description in case of error
    * @return built MessageData or throws an exceptition in case of schema failure
    */
  @throws(classOf[ProtocolException])
  private def buildOrReject(payload: String)(validator: String => Try[Unit])(buildFromJson: String => MessageData )(errMsg: String): MessageData = {
      validator(payload) match {
        case Success(_) => buildFromJson(payload)
        case Failure(e) => throw new ProtocolException(errMsg + ": " + e.getMessage)
      }
  }
}
