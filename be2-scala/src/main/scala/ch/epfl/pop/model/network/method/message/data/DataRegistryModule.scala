package ch.epfl.pop.model.network.method.message.data
import ch.epfl.pop.model.network.method.message.data.ObjectType._
import ch.epfl.pop.model.network.method.message.data.ActionType._
import ch.epfl.pop.model.network.method.message.data.lao.CreateLao
import ch.epfl.pop.model.network.method.message.data.lao.StateLao
import ch.epfl.pop.model.network.method.message.data.lao.UpdateLao
import ch.epfl.pop.model.network.method.message.data.rollCall.CreateRollCall
import ch.epfl.pop.model.network.method.message.data.rollCall.OpenRollCall
import ch.epfl.pop.model.network.method.message.data.rollCall.ReopenRollCall
import ch.epfl.pop.model.network.method.message.data.rollCall.CloseRollCall

object DataRegistryModule {

  final val REGISTRY: DataRegistry = {
    val builder =  DataRegistry.Builder()

    //LAO
    builder.add(LAO, CREATE, DataSchemaValidator.validateSchema(LAO)(CREATE), CreateLao.buildFromJson)
    builder.add(LAO, UPDATE_PROPERTIES, DataSchemaValidator.validateSchema(LAO)(UPDATE_PROPERTIES), UpdateLao.buildFromJson)
    builder.add(LAO, STATE, DataSchemaValidator.validateSchema(LAO)(STATE), StateLao.buildFromJson)

    //RollCall
    builder.add(ROLL_CALL, CREATE, DataSchemaValidator.validateSchema(ROLL_CALL)(CREATE), CreateRollCall.buildFromJson)
    builder.add(ROLL_CALL, OPEN, DataSchemaValidator.validateSchema(ROLL_CALL)(OPEN), OpenRollCall.buildFromJson)
    builder.add(ROLL_CALL, REOPEN, DataSchemaValidator.validateSchema(ROLL_CALL)(REOPEN), ReopenRollCall.buildFromJson)
    builder.add(ROLL_CALL, CLOSE, DataSchemaValidator.validateSchema(ROLL_CALL)(CLOSE), CloseRollCall.buildFromJson)

    //TODO: add other object/action entries
    builder.build
  }
}
