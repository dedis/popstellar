package ch.epfl.pop.pubsub

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.election.{CastVoteElection, EndElection, ResultElection, SetupElection}
import ch.epfl.pop.model.network.method.message.data.lao.{CreateLao, StateLao, UpdateLao}
import ch.epfl.pop.model.network.method.message.data.meeting.{CreateMeeting, StateMeeting}
import ch.epfl.pop.model.network.method.message.data.rollCall.{CloseRollCall, CreateRollCall, OpenRollCall, ReopenRollCall}
import ch.epfl.pop.model.network.method.message.data.socialMedia._
import ch.epfl.pop.model.network.method.message.data.witness.WitnessMessage
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.pubsub.MessageRegistry.RegisterEntry
import ch.epfl.pop.pubsub.graph.GraphMessage
import ch.epfl.pop.pubsub.graph.handlers._
import ch.epfl.pop.pubsub.graph.validators._


final class MessageRegistry(private val register: Map[(ObjectType, ActionType), RegisterEntry]) {
  def getBuilder(_object: ObjectType, action: ActionType): Option[String => MessageData] = register.get((_object, action)).map(_.builder)
  def getValidator(_object: ObjectType, action: ActionType): Option[JsonRpcRequest => GraphMessage] = register.get((_object, action)).map(_.validator)
  def getHandler(_object: ObjectType, action: ActionType): Option[JsonRpcRequest => GraphMessage] = register.get((_object, action)).map(_.handler)
}


object MessageRegistry {
  private final case class RegisterEntry(
                                          builder: String => MessageData,
                                          validator: JsonRpcRequest => GraphMessage,
                                          handler: JsonRpcRequest => GraphMessage,
                                        )

  private final class Register {
    private val elements = collection.immutable.Map.newBuilder[(ObjectType, ActionType), RegisterEntry]

    def add(
             key: (ObjectType, ActionType),
             builder: String => MessageData,
             validator: JsonRpcRequest => GraphMessage,
             handler: JsonRpcRequest => GraphMessage
           ): Unit = {
      elements += (key -> RegisterEntry(builder, validator, handler))
    }

    def get: Map[(ObjectType, ActionType), RegisterEntry] = elements.result()
  }


  def apply(): MessageRegistry = {
    val register: Register = new Register()

    // data lao
    register.add((ObjectType.LAO, ActionType.CREATE), CreateLao.buildFromJson, LaoValidator.validateCreateLao, LaoHandler.handleCreateLao)
    register.add((ObjectType.LAO, ActionType.STATE), StateLao.buildFromJson, LaoValidator.validateStateLao, LaoHandler.handleStateLao)
    register.add((ObjectType.LAO, ActionType.UPDATE_PROPERTIES), UpdateLao.buildFromJson, LaoValidator.validateUpdateLao, LaoHandler.handleUpdateLao)

    // data meeting
    register.add((ObjectType.MEETING, ActionType.CREATE), CreateMeeting.buildFromJson, MeetingValidator.validateCreateMeeting, MeetingHandler.handleCreateMeeting)
    register.add((ObjectType.MEETING, ActionType.STATE), StateMeeting.buildFromJson, MeetingValidator.validateStateMeeting, MeetingHandler.handleStateMeeting)

    // data roll call
    register.add((ObjectType.ROLL_CALL, ActionType.CREATE), CreateRollCall.buildFromJson, RollCallValidator.validateCreateRollCall, RollCallHandler.handleCreateRollCall)
    register.add((ObjectType.ROLL_CALL, ActionType.OPEN), OpenRollCall.buildFromJson, r => RollCallValidator.validateOpenRollCall(r), RollCallHandler.handleOpenRollCall)
    register.add((ObjectType.ROLL_CALL, ActionType.REOPEN), ReopenRollCall.buildFromJson, RollCallValidator.validateReopenRollCall, RollCallHandler.handleReopenRollCall)
    register.add((ObjectType.ROLL_CALL, ActionType.CLOSE), CloseRollCall.buildFromJson, RollCallValidator.validateCloseRollCall, RollCallHandler.handleCloseRollCall)

    // data election
    register.add((ObjectType.ELECTION, ActionType.SETUP), SetupElection.buildFromJson, ElectionValidator.validateSetupElection, ElectionHandler.handleSetupElection)
    register.add((ObjectType.ELECTION, ActionType.CAST_VOTE), CastVoteElection.buildFromJson, ElectionValidator.validateCastVoteElection, ElectionHandler.handleCastVoteElection)
    register.add((ObjectType.ELECTION, ActionType.RESULT), ResultElection.buildFromJson, ElectionValidator.validateResultElection, ElectionHandler.handleResultElection)
    register.add((ObjectType.ELECTION, ActionType.END), EndElection.buildFromJson, ElectionValidator.validateEndElection, ElectionHandler.handleEndElection)

    // data witness
    register.add((ObjectType.MESSAGE, ActionType.WITNESS), WitnessMessage.buildFromJson, WitnessValidator.validateWitnessMessage, WitnessHandler.handleWitnessMessage)

    // data social media
    register.add((ObjectType.CHIRP, ActionType.ADD), AddChirp.buildFromJson, SocialMediaValidator.validateAddChirp, SocialMediaHandler.handleAddChirp)
    register.add((ObjectType.CHIRP, ActionType.DELETE), DeleteChirp.buildFromJson, SocialMediaValidator.validateDeleteChirp, SocialMediaHandler.handleDeleteChirp)
    register.add((ObjectType.CHIRP, ActionType.NOTIFY_ADD), NotifyAddChirp.buildFromJson, SocialMediaValidator.validateNotifyAddChirp, SocialMediaHandler.handleNotifyAddChirp)
    register.add((ObjectType.CHIRP, ActionType.NOTIFY_DELETE), NotifyDeleteChirp.buildFromJson, SocialMediaValidator.validateNotifyDeleteChirp, SocialMediaHandler.handleNotifyDeleteChirp)

    register.add((ObjectType.REACTION, ActionType.ADD), AddReaction.buildFromJson, SocialMediaValidator.validateAddReaction, SocialMediaHandler.handleAddReaction)
    register.add((ObjectType.REACTION, ActionType.DELETE), DeleteReaction.buildFromJson, SocialMediaValidator.validateDeleteReaction, SocialMediaHandler.handleDeleteReaction)

    new MessageRegistry(register.get)
  }
}
