package com.github.dedis.popstellar.model.network.serializer;

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.networknt.schema.*;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Json utility class */
public final class JsonUtils {

  public static final String JSON_RPC = "jsonrpc";
  public static final String JSON_RPC_VERSION = "2.0";

  public static final String JSON_REQUEST_ID = "id";

  private static final String TAG = JsonUtils.class.getSimpleName();
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final JsonSchemaFactory FACTORY =
      JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

  public static final String ROOT_SCHEMA = "protocol/jsonRPC.json";
  public static final String GENERAL_MESSAGE_SCHEMA = "protocol/query/method/message/message.json";
  public static final String DATA_SCHEMA = "protocol/query/method/message/data/data.json";
  public static final String CONNECT_TO_LAO_SCHEMA = "protocol/qrcode/connect_to_lao.json";
  public static final String POP_TOKEN_SCHEME = "protocol/qrcode/pop_token.json";
  public static final String MAIN_PK_SCHEME = "protocol/qrcode/identity.json";
  private static final Map<String, JsonSchema> schemas = new ConcurrentHashMap<>();

  private JsonUtils() {}

  /**
   * Test the JsonRPC version of the given object
   *
   * @param object we want to check the version of
   * @throws JsonParseException if the version cannot be found or it does not match the expected
   *     value
   */
  public static void testRPCVersion(JsonObject object) throws JsonParseException {
    if (!object.has(JSON_RPC)) {
      throw new JsonParseException("Unable to find jsonrpc version");
    }

    testRPCVersion(object.get(JSON_RPC).getAsString());
  }

  /**
   * Test the JsonRPC version with the given value
   *
   * @param version we want to check
   * @throws JsonParseException if the version does not match the expected value
   */
  public static void testRPCVersion(String version) throws JsonParseException {
    if (!version.equals(JSON_RPC_VERSION)) {
      throw new JsonParseException("Unable to parse jsonrpc version : " + version);
    }
  }

  /**
   * Verify the json against the given schema
   *
   * @param schemaPath the path of the schema resource
   * @param json a string representing the json
   * @throws JsonParseException if the json is invalid or cannot be parsed
   */
  public static void verifyJson(String schemaPath, String json) throws JsonParseException {
    Log.d(TAG, "verifyJson for : " + json);

    JsonSchema schema = loadSchema(schemaPath);

    try {
      Set<ValidationMessage> errors = schema.validate(OBJECT_MAPPER.readTree(json));
      if (!errors.isEmpty()) {
        throw new JsonParseException(
            "ValidationMessage errors : " + Arrays.toString(errors.toArray()));
      }
    } catch (JsonProcessingException e) {
      throw new JsonParseException(e);
    }
  }

  /**
   * Load a json schema from the resources directory
   *
   * @param resourcePath relative path inside resources directory
   * @return the JsonSchema
   */
  public static JsonSchema loadSchema(String resourcePath) {
    return schemas.computeIfAbsent(
        resourcePath, k -> FACTORY.getSchema(URI.create("resource:/" + resourcePath)));
  }
}
