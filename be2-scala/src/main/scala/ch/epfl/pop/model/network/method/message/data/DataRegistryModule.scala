package ch.epfl.pop.model.network.method.message.data
import ch.epfl.pop.model.network.method.message.data.ObjectType._
import ch.epfl.pop.model.network.method.message.data.ActionType._
import ch.epfl.pop.model.network.method.message.data.lao.{CreateLao, StateLao, UpdateLao}
import ch.epfl.pop.model.network.method.message.data.rollCall.{CreateRollCall, OpenRollCall, ReopenRollCall, CloseRollCall}
import ch.epfl.pop.model.network.method.message.data.socialMedia._


object DataRegistryModule {

  final val REGISTRY: DataRegistry = {
    val builder =  DataRegistry.Builder()

    //LAO
    builder.add(LAO, CREATE, DataSchemaValidator.validateSchema(LAO)(CREATE), CreateLao.buildFromJson)("CreateLao data could not be parsed")
    builder.add(LAO, UPDATE_PROPERTIES, DataSchemaValidator.validateSchema(LAO)(UPDATE_PROPERTIES), UpdateLao.buildFromJson)("UpdateLao data could not be parsed")
    builder.add(LAO, STATE, DataSchemaValidator.validateSchema(LAO)(STATE), StateLao.buildFromJson)("StateLao data could not be parsed")

    //RollCall
    builder.add(ROLL_CALL, CREATE, DataSchemaValidator.validateSchema(ROLL_CALL)(CREATE), CreateRollCall.buildFromJson)("CreateRollCall data could not be parsed")
    builder.add(ROLL_CALL, OPEN, DataSchemaValidator.validateSchema(ROLL_CALL)(OPEN), OpenRollCall.buildFromJson)("OpenRollCall data could not be parsed")
    builder.add(ROLL_CALL, REOPEN, DataSchemaValidator.validateSchema(ROLL_CALL)(REOPEN), ReopenRollCall.buildFromJson)("ReOpenRollCall data could not be parsed")
    builder.add(ROLL_CALL, CLOSE, DataSchemaValidator.validateSchema(ROLL_CALL)(CLOSE), CloseRollCall.buildFromJson)("CloseRollCall data could not be parsed")

    //Social Media
    builder.add(CHIRP, ADD,  DataSchemaValidator.validateSchema(CHIRP)(ADD), AddChirp.buildFromJson)("AddChirp could not be parsed")
    builder.add(CHIRP, NOTIFY_ADD, DataSchemaValidator.validateSchema(CHIRP)(NOTIFY_ADD), NotifyAddChirp.buildFromJson)("NotifyAddChirp could not be parsed")
    builder.add(CHIRP, DELETE,  DataSchemaValidator.validateSchema(CHIRP)(DELETE), DeleteChirp.buildFromJson)("DeleteChirp could not be parsed")
    builder.add(CHIRP, NOTIFY_DELETE, DataSchemaValidator.validateSchema(CHIRP)(NOTIFY_DELETE), NotifyDeleteChirp.buildFromJson)("NotifyDeleteChirp could not be parsed")

    //TODO: add other object/action entries
    builder.build
  }
}
