package com.github.dedis.popstellar.model.network.serializer

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import timber.log.Timber
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/** Json utility class  */
object JsonUtils {
    const val JSON_RPC = "jsonrpc"
    const val JSON_RPC_VERSION = "2.0"
    const val JSON_REQUEST_ID = "id"
    private val TAG = JsonUtils::class.java.simpleName
    private val OBJECT_MAPPER = ObjectMapper()
    private val FACTORY = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
    const val ROOT_SCHEMA = "protocol/jsonRPC.json"
    const val GENERAL_MESSAGE_SCHEMA = "protocol/query/method/message/message.json"
    const val DATA_SCHEMA = "protocol/query/method/message/data/data.json"
    const val CONNECT_TO_LAO_SCHEMA = "protocol/qrcode/connect_to_lao.json"
    const val POP_TOKEN_SCHEME = "protocol/qrcode/pop_token.json"
    const val MAIN_PK_SCHEME = "protocol/qrcode/main_public_key.json"
    private val schemas: MutableMap<String, JsonSchema> = ConcurrentHashMap()

    /**
     * Test the JsonRPC version of the given object
     *
     * @param object we want to check the version of
     * @throws JsonParseException if the version cannot be found or it does not match the expected
     * value
     */
    @JvmStatic
    @Throws(JsonParseException::class)
    fun testRPCVersion(`object`: JsonObject) {
        if (!`object`.has(JSON_RPC)) {
            throw JsonParseException("Unable to find jsonrpc version")
        }
        testRPCVersion(`object`[JSON_RPC].asString)
    }

    /**
     * Test the JsonRPC version with the given value
     *
     * @param version we want to check
     * @throws JsonParseException if the version does not match the expected value
     */
    @JvmStatic
    @Throws(JsonParseException::class)
    fun testRPCVersion(version: String) {
        if (version != JSON_RPC_VERSION) {
            throw JsonParseException("Unable to parse jsonrpc version : $version")
        }
    }

    /**
     * Verify the json against the given schema
     *
     * @param schemaPath the path of the schema resource
     * @param json a string representing the json
     * @throws JsonParseException if the json is invalid or cannot be parsed
     */
    @JvmStatic
    @Throws(JsonParseException::class)
    fun verifyJson(schemaPath: String, json: String) {
        Timber.tag(TAG).d("verifyJson for : %s", json)
        val schema = loadSchema(schemaPath)
        try {
            val errors = schema.validate(OBJECT_MAPPER.readTree(json))
            if (errors.isNotEmpty()) {
                throw JsonParseException(
                        """
                            Json : $json
                            ValidationMessage errors : ${errors.toTypedArray().contentToString()}
                            """.trimIndent())
            }
        } catch (e: JsonProcessingException) {
            throw JsonParseException(e)
        }
    }

    /**
     * Load a json schema from the resources directory
     *
     * @param resourcePath relative path inside resources directory
     * @return the JsonSchema
     */
    @JvmStatic
    fun loadSchema(resourcePath: String): JsonSchema {
        return schemas.computeIfAbsent(
                resourcePath) { FACTORY.getSchema(URI.create("resource:/$resourcePath")) }
    }
}