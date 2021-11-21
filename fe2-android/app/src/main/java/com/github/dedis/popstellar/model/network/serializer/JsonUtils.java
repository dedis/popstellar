package com.github.dedis.popstellar.model.network.serializer;

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Answer;
import com.github.dedis.popstellar.model.network.method.Message;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

/** Json utility class */
public final class JsonUtils {

  public static final String JSON_RPC = "jsonrpc";
  public static final String JSON_RPC_VERSION = "2.0";

  public static final String JSON_REQUEST_ID = "id";

  private static final String TAG = JsonUtils.class.getSimpleName();
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final JsonSchema SCHEMA;

  static {
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    InputStream is =
        Objects.requireNonNull(Thread.currentThread().getContextClassLoader())
            .getResourceAsStream("protocol/jsonRPC.json");
    SCHEMA = factory.getSchema(is);
  }

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
   * Create a Gson object with the needed serializer for the app usage
   *
   * @return the Gson object
   */
  @Deprecated
  public static Gson createGson() {
    return new GsonBuilder()
        .registerTypeAdapter(GenericMessage.class, new JsonGenericMessageDeserializer())
        .registerTypeAdapter(Message.class, new JsonMessageSerializer())
        .registerTypeAdapter(Data.class, new JsonDataSerializer())
        .registerTypeAdapter(Answer.class, new JsonAnswerSerializer())
        .create();
  }

  /**
   * Verify the json against the root schema
   *
   * @param json a string representing the json
   * @throws JsonParseException if the json is invalid or cannot be parsed
   */
  public static void verifyJson(String json) throws JsonParseException {
    Log.d(TAG, "verifyJson for : " + json);

    try {
      Set<ValidationMessage> errors = SCHEMA.validate(OBJECT_MAPPER.readTree(json));
      if (!errors.isEmpty()) {
        throw new JsonParseException(
            "ValidationMessage errors : " + Arrays.toString(errors.toArray()));
      }
    } catch (JsonProcessingException e) {
      throw new JsonParseException(e);
    }
  }
}
