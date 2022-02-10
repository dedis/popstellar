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
import ch.epfl.pop.pubsub.MessageRegistry.DataHeader
import ch.epfl.pop.pubsub.graph.GraphMessage
import ch.epfl.pop.pubsub.graph.handlers._
import ch.epfl.pop.pubsub.graph.validators._


final class MessageRegistry(
                             private val builderRegister: Map[DataHeader, String => MessageData],
                             private val validatorRegister: Map[DataHeader, JsonRpcRequest => GraphMessage],
                             private val handlerRegister: Map[DataHeader, JsonRpcRequest => GraphMessage],
                           ) {
  private def get[U, V](_object: ObjectType, action: ActionType, register: Map[DataHeader, U => V]): Option[U => V] = {
    register.get((_object, action))
  }

  def getBuilder(_object: ObjectType, action: ActionType): Option[String => MessageData] = get(_object, action, builderRegister)
  def getValidator(_object: ObjectType, action: ActionType): Option[JsonRpcRequest => GraphMessage] = get(_object, action, validatorRegister)
  def getHandler(_object: ObjectType, action: ActionType): Option[JsonRpcRequest => GraphMessage] = get(_object, action, handlerRegister)
}


object MessageRegistry {
  private type DataHeader = (ObjectType, ActionType)

  private final class Register[T] {
    private val elements = collection.immutable.Map.newBuilder[DataHeader, T]
    def add(key: DataHeader, entry: T): Unit = elements += (key -> entry)
    def get: Map[DataHeader, T] = elements.result()
  }

  private final class RegisterCollection() {
    val builderRegister = new Register[String => MessageData]
    val validatorRegister = new Register[JsonRpcRequest => GraphMessage]
    val handlerRegister = new Register[JsonRpcRequest => GraphMessage]

    def add(key: DataHeader, builder: String => MessageData, validator: JsonRpcRequest => GraphMessage, handler: JsonRpcRequest => GraphMessage): Unit = {
      builderRegister.add(key, builder)
      validatorRegister.add(key, validator)
      handlerRegister.add(key, handler)
    }
    def get = (builderRegister.get, validatorRegister.get, handlerRegister.get)
  }


  def apply(): MessageRegistry = {
    def build(builderRegister: Map[DataHeader, String => MessageData], validatorRegister: Map[DataHeader, JsonRpcRequest => GraphMessage], handlerRegister: Map[DataHeader, JsonRpcRequest => GraphMessage]): MessageRegistry = {
      new MessageRegistry(builderRegister, validatorRegister, handlerRegister)
    }

    val registers: RegisterCollection = new RegisterCollection()

    // data lao
    registers.add((ObjectType.LAO, ActionType.CREATE), CreateLao.buildFromJson, LaoValidator.validateCreateLao, LaoHandler.handleCreateLao)
    registers.add((ObjectType.LAO, ActionType.STATE), StateLao.buildFromJson, LaoValidator.validateStateLao, LaoHandler.handleStateLao)
    registers.add((ObjectType.LAO, ActionType.UPDATE_PROPERTIES), UpdateLao.buildFromJson, LaoValidator.validateUpdateLao, LaoHandler.handleUpdateLao)

    // data meeting
    registers.add((ObjectType.MEETING, ActionType.CREATE), CreateMeeting.buildFromJson, MeetingValidator.validateCreateMeeting, MeetingHandler.handleCreateMeeting)
    registers.add((ObjectType.MEETING, ActionType.STATE), StateMeeting.buildFromJson, MeetingValidator.validateStateMeeting, MeetingHandler.handleStateMeeting)

    // data roll call
    registers.add((ObjectType.ROLL_CALL, ActionType.CREATE), CreateRollCall.buildFromJson, RollCallValidator.validateCreateRollCall, RollCallHandler.handleCreateRollCall)
    registers.add((ObjectType.ROLL_CALL, ActionType.OPEN), OpenRollCall.buildFromJson, r => RollCallValidator.validateOpenRollCall(r), RollCallHandler.handleOpenRollCall)
    registers.add((ObjectType.ROLL_CALL, ActionType.REOPEN), ReopenRollCall.buildFromJson, RollCallValidator.validateReopenRollCall, RollCallHandler.handleReopenRollCall)
    registers.add((ObjectType.ROLL_CALL, ActionType.CLOSE), CloseRollCall.buildFromJson, RollCallValidator.validateCloseRollCall, RollCallHandler.handleCloseRollCall)

    // data election
    registers.add((ObjectType.ELECTION, ActionType.SETUP), SetupElection.buildFromJson, ElectionValidator.validateSetupElection, ElectionHandler.handleSetupElection)
    registers.add((ObjectType.ELECTION, ActionType.CAST_VOTE), CastVoteElection.buildFromJson, ElectionValidator.validateCastVoteElection, ElectionHandler.handleCastVoteElection)
    registers.add((ObjectType.ELECTION, ActionType.RESULT), ResultElection.buildFromJson, ElectionValidator.validateResultElection, ElectionHandler.handleResultElection)
    registers.add((ObjectType.ELECTION, ActionType.END), EndElection.buildFromJson, ElectionValidator.validateEndElection, ElectionHandler.handleEndElection)

    // data witness
    registers.add((ObjectType.MESSAGE, ActionType.WITNESS), WitnessMessage.buildFromJson, WitnessValidator.validateWitnessMessage, WitnessHandler.handleWitnessMessage)

    // data social media
    registers.add((ObjectType.CHIRP, ActionType.ADD), AddChirp.buildFromJson, SocialMediaValidator.validateAddChirp, SocialMediaHandler.handleAddChirp)
    registers.add((ObjectType.CHIRP, ActionType.DELETE), DeleteChirp.buildFromJson, SocialMediaValidator.validateDeleteChirp, SocialMediaHandler.handleDeleteChirp)
    registers.add((ObjectType.CHIRP, ActionType.NOTIFY_ADD), NotifyAddChirp.buildFromJson, SocialMediaValidator.validateNotifyAddChirp, SocialMediaHandler.handleNotifyAddChirp)
    registers.add((ObjectType.CHIRP, ActionType.NOTIFY_DELETE), NotifyDeleteChirp.buildFromJson, SocialMediaValidator.validateNotifyDeleteChirp, SocialMediaHandler.handleNotifyDeleteChirp)

    registers.add((ObjectType.REACTION, ActionType.ADD), AddReaction.buildFromJson, SocialMediaValidator.validateAddReaction, SocialMediaHandler.handleAddReaction)
    registers.add((ObjectType.REACTION, ActionType.DELETE), DeleteReaction.buildFromJson, SocialMediaValidator.validateDeleteReaction, SocialMediaHandler.handleDeleteReaction)

    build _ tupled registers.get // Scala tuple unpacking
  }
}
