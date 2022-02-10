package ch.epfl.pop.pubsub

import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import ch.epfl.pop.model.network.{JsonRpcMessage, JsonRpcRequest}
import ch.epfl.pop.pubsub.MessageRegistry.{DataHeader, HandlerI, ValidatorI}
import ch.epfl.pop.pubsub.graph.GraphMessage
import ch.epfl.pop.pubsub.graph.handlers.{ElectionHandler, LaoHandler, MeetingHandler, RollCallHandler, SocialMediaHandler, WitnessHandler}
import ch.epfl.pop.pubsub.graph.validators._


final class MessageRegistry(
                             private val dataValidatorRegister: Map[DataHeader, ValidatorI],
                             private val handlerRegister: Map[DataHeader, HandlerI],
                           ) {
  private def get(rpc: JsonRpcMessage, register: Map[DataHeader, JsonRpcRequest => GraphMessage]): Option[JsonRpcRequest => GraphMessage] = rpc match {
    case r: JsonRpcRequest => r.getDecodedDataHeader match {
      case Some(header: DataHeader) => register.get(header)
      case _ => None
    }
    case _ => None // depending on consensus, some non-None return values could be added for JsonRpcAnswers
  }

  def getDataValidator(rpc: JsonRpcMessage): Option[ValidatorI] = get(rpc, dataValidatorRegister)
  def getHandler(rpc: JsonRpcMessage): Option[ValidatorI] = get(rpc, handlerRegister)
}


object MessageRegistry {
  private type DataHeader = (ObjectType, ActionType)
  private type ValidatorI = JsonRpcRequest => GraphMessage
  private type HandlerI = JsonRpcRequest => GraphMessage

  private final class Register[T] {
    private val elements = collection.immutable.Map.newBuilder[DataHeader, T]
    def add(key: DataHeader, entry: T): Unit = elements += (key -> entry)
    def get: Map[DataHeader, T] = elements.result()
  }

  private final class RegisterCollection() {
    val validatorRegister: Register[ValidatorI] = new Register[ValidatorI]
    val handlerRegister: Register[HandlerI] = new Register[HandlerI]

    def add(key: DataHeader, validator: ValidatorI, handler: HandlerI): Unit = {
      validatorRegister.add(key, validator)
      handlerRegister.add(key, handler)
    }
    def get = (validatorRegister.get, handlerRegister.get)
  }


  def apply(): MessageRegistry = {
    def build(validatorRegister: Map[DataHeader, ValidatorI], handlerRegister: Map[DataHeader, ValidatorI]): MessageRegistry = {
      new MessageRegistry(validatorRegister, handlerRegister)
    }

    val registers: RegisterCollection = new RegisterCollection()

    // data lao
    registers.add((ObjectType.LAO, ActionType.CREATE), LaoValidator.validateCreateLao, LaoHandler.handleCreateLao)
    registers.add((ObjectType.LAO, ActionType.STATE), LaoValidator.validateStateLao, LaoHandler.handleStateLao)
    registers.add((ObjectType.LAO, ActionType.UPDATE_PROPERTIES), LaoValidator.validateUpdateLao, LaoHandler.handleUpdateLao)

    // data meeting
    registers.add((ObjectType.MEETING, ActionType.CREATE), MeetingValidator.validateCreateMeeting, MeetingHandler.handleCreateMeeting)
    registers.add((ObjectType.MEETING, ActionType.STATE), MeetingValidator.validateStateMeeting, MeetingHandler.handleStateMeeting)

    // data roll call
    registers.add((ObjectType.ROLL_CALL, ActionType.CREATE), RollCallValidator.validateCreateRollCall, RollCallHandler.handleCreateRollCall)
    registers.add((ObjectType.ROLL_CALL, ActionType.OPEN), r => RollCallValidator.validateOpenRollCall(r), RollCallHandler.handleOpenRollCall)
    registers.add((ObjectType.ROLL_CALL, ActionType.REOPEN), RollCallValidator.validateReopenRollCall, RollCallHandler.handleReopenRollCall)
    registers.add((ObjectType.ROLL_CALL, ActionType.CLOSE), RollCallValidator.validateCloseRollCall, RollCallHandler.handleCloseRollCall)

    // data election
    registers.add((ObjectType.ELECTION, ActionType.SETUP), ElectionValidator.validateSetupElection, ElectionHandler.handleSetupElection)
    registers.add((ObjectType.ELECTION, ActionType.CAST_VOTE), ElectionValidator.validateCastVoteElection, ElectionHandler.handleCastVoteElection)
    registers.add((ObjectType.ELECTION, ActionType.RESULT), ElectionValidator.validateResultElection, ElectionHandler.handleResultElection)
    registers.add((ObjectType.ELECTION, ActionType.END), ElectionValidator.validateEndElection, ElectionHandler.handleEndElection)

    // data witness
    registers.add((ObjectType.MESSAGE, ActionType.WITNESS), WitnessValidator.validateWitnessMessage, WitnessHandler.handleWitnessMessage)

    // data social media
    registers.add((ObjectType.CHIRP, ActionType.ADD), SocialMediaValidator.validateAddChirp, SocialMediaHandler.handleAddChirp)
    registers.add((ObjectType.CHIRP, ActionType.DELETE), SocialMediaValidator.validateDeleteChirp, SocialMediaHandler.handleDeleteChirp)
    registers.add((ObjectType.CHIRP, ActionType.NOTIFY_ADD), SocialMediaValidator.validateNotifyAddChirp, SocialMediaHandler.handleNotifyAddChirp)
    registers.add((ObjectType.CHIRP, ActionType.NOTIFY_DELETE), SocialMediaValidator.validateNotifyDeleteChirp, SocialMediaHandler.handleNotifyDeleteChirp)

    registers.add((ObjectType.REACTION, ActionType.ADD), SocialMediaValidator.validateAddReaction, SocialMediaHandler.handleAddReaction)
    registers.add((ObjectType.REACTION, ActionType.DELETE), SocialMediaValidator.validateDeleteReaction, SocialMediaHandler.handleDeleteReaction)

    build _ tupled registers.get // Scala tuple unpacking
  }
}
