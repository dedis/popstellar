package ch.epfl.pop.model.network.method.message.data

import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.pubsub.graph.Validator
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.networknt.schema.JsonSchema
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters.SetHasAsScala
import scala.util.{Failure, Success, Try}


/** DataSchemaValidator Object, provides a validateSchema method that verifies a certain payload of a certain actionType
 * and objectType is conform to the protocol.
 */
object DataSchemaValidator {
  private final val objectMapper: ObjectMapper = new ObjectMapper()
  private final val logger = LoggerFactory.getLogger("DataSchemaValidator")

  /* Paths to Schemas */
  private final val baseDir = "protocol/query/method/message/data/"
  private final val dataCreateLaoPath = baseDir + "dataCreateLao.json"
  private final val dataStateLaoPath = baseDir + "dataStateLao.json"
  private final val dataUpdateLao = baseDir + "dataUpdateLao.json"
  private final val dataCreateRC = baseDir + "dataCreateRollCall.json"
  private final val dataOpenRC = baseDir + "dataOpenRollCall.json"
  private final val dataReopenRC = dataOpenRC
  private final val dataCloseRC = baseDir + "dataCloseRollCall.json"
  private final val dataAddChirp = baseDir + "dataAddChirp.json"
  private final val dataNotifyAddChirp = baseDir + "dataNotifyAddChirp.json"
  private final val dataDeleteChirp = baseDir + "dataDeleteChirp.json"
  private final val dataNotifyDeleteChirp = baseDir + "dataNotifyDeleteChirp.json"

  private final val dataAddReaction = baseDir + "dataAddReaction.json"
  private final val dataDeleteReaction = baseDir + "dataDeleteReaction.json"

  /* Validation Schemas */
  //TODO: Add schemas for other features: Meetings, RollCalls...
  private final lazy val createLaoSchema: JsonSchema = Validator.setupSchemaValidation(dataCreateLaoPath, objectMapper)
  private final lazy val stateLaoSchema: JsonSchema = Validator.setupSchemaValidation(dataStateLaoPath, objectMapper)
  private final lazy val updateLaoSchema: JsonSchema = Validator.setupSchemaValidation(dataUpdateLao, objectMapper)
  private final lazy val createRcSchema: JsonSchema = Validator.setupSchemaValidation(dataCreateRC, objectMapper)
  private final lazy val openRcSchema: JsonSchema = Validator.setupSchemaValidation(dataOpenRC, objectMapper)
  private final lazy val reopenRcSchema = openRcSchema
  private final lazy val closeRcSchema: JsonSchema = Validator.setupSchemaValidation(dataCloseRC, objectMapper)
  private final lazy val addChirpSchema: JsonSchema = Validator.setupSchemaValidation(dataAddChirp, objectMapper)
  private final lazy val notifyAddChirpSchema: JsonSchema = Validator.setupSchemaValidation(dataNotifyAddChirp, objectMapper)
  private final lazy val deleteChirpSchema: JsonSchema = Validator.setupSchemaValidation(dataDeleteChirp, objectMapper)
  private final lazy val notifyDeleteChirpSchema: JsonSchema = Validator.setupSchemaValidation(dataNotifyDeleteChirp, objectMapper)
  private final lazy val addReactionSchema: JsonSchema = Validator.setupSchemaValidation(dataAddReaction, objectMapper)
  private final lazy val deleteReactionSchema: JsonSchema = Validator.setupSchemaValidation(dataDeleteReaction, objectMapper)

  //TODO: Add validation schemas for other features: Meetings, Elections...

  def validateSchema(objType: ObjectType)(actionType: ActionType)(payload: String): Try[Unit] =
    (objType, actionType) match {
      //LAO
      case (ObjectType.LAO, ActionType.CREATE) => validateWithSchema(createLaoSchema)(payload)
      case (ObjectType.LAO, ActionType.STATE) => validateWithSchema(stateLaoSchema)(payload)
      case (ObjectType.LAO, ActionType.UPDATE_PROPERTIES) => validateWithSchema(updateLaoSchema)(payload)

      //RollCall
      case (ObjectType.ROLL_CALL, ActionType.CREATE) => validateWithSchema(createRcSchema)(payload)
      case (ObjectType.ROLL_CALL, ActionType.OPEN) => validateWithSchema(openRcSchema)(payload)
      case (ObjectType.ROLL_CALL, ActionType.REOPEN) => validateWithSchema(reopenRcSchema)(payload)
      case (ObjectType.ROLL_CALL, ActionType.CLOSE) => validateWithSchema(closeRcSchema)(payload)

      //Social Media
      case (ObjectType.CHIRP, ActionType.ADD) => validateWithSchema(addChirpSchema)(payload)
      case (ObjectType.CHIRP, ActionType.NOTIFY_ADD) => validateWithSchema(notifyAddChirpSchema)(payload)
      case (ObjectType.CHIRP, ActionType.DELETE) => validateWithSchema(deleteChirpSchema)(payload)
      case (ObjectType.CHIRP, ActionType.NOTIFY_DELETE) => validateWithSchema(notifyDeleteChirpSchema)(payload)

      case (ObjectType.REACTION, ActionType.ADD) => validateWithSchema(addReactionSchema)(payload)
      case (ObjectType.REACTION, ActionType.DELETE) => validateWithSchema(deleteReactionSchema)(payload)

      //TODO:Add other cases
      case _ =>
        logger.error("Schema for data message could not be verified or data of unknown type")
        Failure(new ProtocolException("Schema for data message could not be verified or data of unknown type"))
    }

  /** Validates a certain payload to match a given predefined schema
   *
   * @param schema  json schema to match against the payload
   * @param payload payload to verify
   * @return Success if the validation succeeds Failure with an exception if it fails
   */
  private def validateWithSchema(schema: JsonSchema)(payload: String): Try[Unit] = {
    val jsonNode: JsonNode = objectMapper.readTree(payload)
    val errors = schema.validate(jsonNode).asScala
    errors match {
      case _ if errors.isEmpty => Success((): Unit)
      case _ => Failure(new Exception(errors.mkString("; ")))
    }
  }
}
