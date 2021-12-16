package ch.epfl.pop.model.network.method.message.data

import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.pubsub.graph.Validator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchema
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util.Failure
import scala.util.Success
import scala.util.Try

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

  /* Validation Schemas */
  //TODO: Add schemas for other features: Meetings, RollCalls...
  private final lazy val createLaoSchema: JsonSchema = Validator.setupSchemaValidation(dataCreateLaoPath, objectMapper)
  private final lazy val stateLaoSchema: JsonSchema = Validator.setupSchemaValidation(dataStateLaoPath, objectMapper)
  private final lazy val updateLaoSchema: JsonSchema = Validator.setupSchemaValidation(dataUpdateLao, objectMapper)

  //TODO: Add validaton schemas for other features: Meetings, RollCalls...
  def validateSchema(objType: ObjectType)(actionType: ActionType)(payload: String): Try[Unit] =
    (objType, actionType) match {
      case (ObjectType.LAO, ActionType.CREATE)            => validateWithSchema(createLaoSchema)(payload)
      case (ObjectType.LAO, ActionType.STATE)             => validateWithSchema(stateLaoSchema)(payload)
      case (ObjectType.LAO, ActionType.UPDATE_PROPERTIES) => validateWithSchema(updateLaoSchema)(payload)
      case _ =>
        logger.error("Schema for data message could not be verified or data of unknown type")
        Failure(new ProtocolException("Schema for data message could not be verified or data of unknown type"))
    }

  /** Validates a certain payload to match a given predifined schema
    * @param schema
    * @param payload
    *   payload to verify
    * @return
    *   Success if the validation succeeds Failure with a exception if it fails
    */
  private def validateWithSchema(schema: JsonSchema)(payload: String): Try[Unit] = {
    val jsonNode: JsonNode = objectMapper.readTree(payload)
    val errors = schema.validate(jsonNode).asScala
    errors match {
      case _ if errors.isEmpty => Success()
      case _                   => Failure(new Exception(errors.mkString("; ")))
    }
  }
}
