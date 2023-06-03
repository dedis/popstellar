package com.github.dedis.popstellar.model.network.serializer.network;

import com.github.dedis.popstellar.model.network.answer.Error;
import com.github.dedis.popstellar.model.network.answer.*;
import com.github.dedis.popstellar.model.network.serializer.JsonUtils;
import com.google.gson.*;

import java.lang.reflect.Type;

/** Json serializer and deserializer for the answer */
public class JsonAnswerSerializer implements JsonSerializer<Answer>, JsonDeserializer<Answer> {

  private static final String RESULT = "result";
  private static final String ERROR = "error";

  @Override
  public Answer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject obj = json.getAsJsonObject();
    JsonUtils.testRPCVersion(obj);
    JsonUtils.verifyJson(JsonUtils.ROOT_SCHEMA, json.toString());

    if (obj.has(RESULT)) {
      return context.deserialize(json, Result.class);
    } else if (obj.has(ERROR)) {
      return context.deserialize(json, Error.class);
    } else {
      throw new JsonParseException("A result must contain one of the field result or error");
    }
  }

  @Override
  public JsonElement serialize(Answer src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject obj = context.serialize(src).getAsJsonObject();
    obj.addProperty(JsonUtils.JSON_RPC, JsonUtils.JSON_RPC_VERSION);
    JsonUtils.verifyJson(JsonUtils.ROOT_SCHEMA, obj.toString());
    return obj;
  }
}
