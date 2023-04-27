package ch.epfl.pop.pubsub.graph

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.method.message.data.ProtocolException
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.networknt.schema._
import spray.json._

import java.io.InputStream
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object SchemaVerifier {
  private final val objectMapper: ObjectMapper = new ObjectMapper()

  private final val jsonSchemaPath = "protocol/jsonRPC.json" // with respect to resources folder
  private final val dataSchemasPath = "protocol/query/method/message/data" // with respect to resources folder

  private final val jsonSchema: JsonSchema = setupSchemaVerification(jsonSchemaPath)

  def setupSchemaVerification(jsonPath: String): JsonSchema = {
    // get input stream of protocol's query.json file from resources folder
    def queryFile: InputStream = this.getClass.getClassLoader.getResourceAsStream(jsonPath)

    // creation of a JsonSchemaFactory that supports the DraftV07 with the schema obtained from a node created from query.json
    val factory: JsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)

    // creation of a JsonNode using the readTree function from the file query.json (at queryPath)
    // closing the stream is done by readTree
    // FIXME: error handling for queryPath
    lazy val jsonNode: JsonNode = objectMapper.readTree(queryFile)

    // creation of a JsonSchema from the previously created factory and JsonNode
    factory.getSchema(jsonNode)
  }

  private def verifySchema(schema: JsonSchema, jsonString: JsonString): Try[Unit] = {
    try {
      // in case of invalid JsonString, we catch the exception thrown by the readTree and answer with an error
      // creation of a JsonNode containing the information from the input JSON string
      val jsonNode: JsonNode = objectMapper.readTree(jsonString)
      // validation of the input, the result is a set of errors (if no errors, the set is empty)
      // Note: the library is written in Java, thus we convert the Java Set<T> into a Scala Set[T]
      val errors: Set[ValidationMessage] = schema.validate(jsonNode).asScala.toSet
      errors match {
        case _ if errors.isEmpty => Success((): Unit)
        case _                   => Failure(new ProtocolException(errors.mkString("; "))) // concatenate all schema validation errors into one
      }
    } catch {
      case e: Exception => Failure(new ProtocolException("Invalid object detected, JSON object expected."))
    }
  }

  /** Validates a high-level JSON-rpc json string (i.e. *NOT* the data field)
    *
    * @param jsonString
    *   JSON string representation of the message
    * @return
    *   a [[GraphMessage]] containing the input if successful, or a [[PipelineError]] otherwise
    */
  def verifyRpcSchema(jsonString: JsonString): Either[PipelineError, JsonString] = {
    verifySchema(jsonSchema, jsonString) match {
      case Success(_) => Right(jsonString)
      case Failure(ex) =>
        val rpcId = Try(jsonString.parseJson.asJsObject.getFields("id")) match {
          case Success(Seq(JsNumber(id))) => Some(id.toInt)
          case _                          => None
        }
        Left(PipelineError(ErrorCodes.INVALID_DATA.id, ex.getMessage, rpcId))
    }
  }

  /** Verifies a low-level data json string
    * @note
    *   use the MessageRegistry to access this function easily
    *
    * @param schema
    *   schema that should be used to verify the jsonString
    * @param jsonString
    *   JSON string representation of the data field
    * @return
    *   a Success or a Failure depending whether the validation succeeded or not
    */
  def verifyDataSchema(schema: JsonSchema)(jsonString: JsonString): Try[Unit] = verifySchema(schema, jsonString)

  /** Creates a SchemaVerifier function from the corresponding JsonSchema verifier file
    * @note
    *   used by the MessageRegistry to ease the process of SchemaVerifier functions creation
    *
    * @param schemaFileName
    *   name of the corresponding schema file wrt. dataSchemasPath
    * @return
    *   a schema validator taking a json string and returning whether the string is conform to the schema file or not
    */
  def createSchemaVerifier(schemaFileName: String): JsonString => Try[Unit] = verifyDataSchema(
    setupSchemaVerification(s"$dataSchemasPath/$schemaFileName")
  )

  // takes a string (json) input and compares it with the JsonSchema
  val rpcSchemaVerifier: Flow[JsonString, Either[PipelineError, JsonString], NotUsed] = Flow[JsonString].map(verifyRpcSchema)
}
