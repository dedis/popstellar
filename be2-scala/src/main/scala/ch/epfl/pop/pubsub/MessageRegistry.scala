package ch.epfl.pop.pubsub

import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import ch.epfl.pop.model.network.{JsonRpcMessage, JsonRpcRequest}
import ch.epfl.pop.pubsub.MessageRegistry.{DataHeader, ValidatorI}
import ch.epfl.pop.pubsub.graph.GraphMessage
import ch.epfl.pop.pubsub.graph.validators._


final class MessageRegistry(
                             private val dataValidatorRegister: Map[DataHeader, ValidatorI],
                           ) {
  def getDataValidator(rpc: JsonRpcMessage): Option[ValidatorI] = rpc match {
    case r: JsonRpcRequest => r.getDecodedDataHeader match {
      case Some(header: DataHeader) => dataValidatorRegister.get(header)
      case _ =>
        print(s"MessageRegistry 'getDataValidator' was called on a JsonRpcRequest without decoded data header")
        None
    }
    case _ => None // depending on consensus, some non-None return values could be added for JsonRpcAnswers
  }
}


object MessageRegistry {
  private type DataHeader = (ObjectType, ActionType)
  private type ValidatorI = JsonRpcRequest => GraphMessage

  private class Register[T] {
    private val elements = collection.immutable.Map.newBuilder[DataHeader, T]
    def add(key: DataHeader, entry: T): Unit = elements += (key -> entry)
    def get: Map[DataHeader, T] = elements.result()
  }

  def apply(): MessageRegistry = {
    val dataValidatorRegister: Register[ValidatorI] = new Register[ValidatorI]

    // data lao
    dataValidatorRegister.add((ObjectType.LAO, ActionType.CREATE), LaoValidator.validateCreateLao)
    dataValidatorRegister.add((ObjectType.LAO, ActionType.STATE), LaoValidator.validateStateLao)
    dataValidatorRegister.add((ObjectType.LAO, ActionType.UPDATE_PROPERTIES), LaoValidator.validateUpdateLao)

    // data meeting
    dataValidatorRegister.add((ObjectType.MEETING, ActionType.CREATE), MeetingValidator.validateCreateMeeting)
    dataValidatorRegister.add((ObjectType.MEETING, ActionType.STATE), MeetingValidator.validateStateMeeting)

    // data roll call
    dataValidatorRegister.add((ObjectType.ROLL_CALL, ActionType.CREATE), RollCallValidator.validateCreateRollCall)
    dataValidatorRegister.add((ObjectType.ROLL_CALL, ActionType.OPEN), r => RollCallValidator.validateOpenRollCall(r))
    dataValidatorRegister.add((ObjectType.ROLL_CALL, ActionType.REOPEN), RollCallValidator.validateReopenRollCall)
    dataValidatorRegister.add((ObjectType.ROLL_CALL, ActionType.CLOSE), RollCallValidator.validateCloseRollCall)

    // data election
    dataValidatorRegister.add((ObjectType.ELECTION, ActionType.SETUP), ElectionValidator.validateSetupElection)
    dataValidatorRegister.add((ObjectType.ELECTION, ActionType.CAST_VOTE), ElectionValidator.validateCastVoteElection)
    dataValidatorRegister.add((ObjectType.ELECTION, ActionType.RESULT), ElectionValidator.validateResultElection)
    dataValidatorRegister.add((ObjectType.ELECTION, ActionType.END), ElectionValidator.validateEndElection)

    // data witness
    dataValidatorRegister.add((ObjectType.MESSAGE, ActionType.WITNESS), WitnessValidator.validateWitnessMessage)

    // data social media
    dataValidatorRegister.add((ObjectType.CHIRP, ActionType.ADD), SocialMediaValidator.validateAddChirp)
    dataValidatorRegister.add((ObjectType.CHIRP, ActionType.DELETE), SocialMediaValidator.validateDeleteChirp)
    dataValidatorRegister.add((ObjectType.CHIRP, ActionType.NOTIFY_ADD), SocialMediaValidator.validateNotifyAddChirp)
    dataValidatorRegister.add((ObjectType.CHIRP, ActionType.NOTIFY_DELETE), SocialMediaValidator.validateNotifyDeleteChirp)

    dataValidatorRegister.add((ObjectType.REACTION, ActionType.ADD), SocialMediaValidator.validateAddReaction)
    dataValidatorRegister.add((ObjectType.REACTION, ActionType.DELETE), SocialMediaValidator.validateDeleteReaction)


    new MessageRegistry(dataValidatorRegister.get)
  }
}
