package com.github.dedis.popstellar.model.network.serializer;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Answer;
import com.github.dedis.popstellar.model.network.method.Message;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/** Json utility class */
public final class JsonUtils {

  public static final String JSON_RPC = "jsonrpc";
  public static final String JSON_RPC_VERSION = "2.0";

  public static final String JSON_REQUEST_ID = "id";

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
}
