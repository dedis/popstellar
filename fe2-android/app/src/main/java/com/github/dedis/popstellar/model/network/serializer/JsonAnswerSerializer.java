package com.github.dedis.popstellar.model.network.serializer;

import com.github.dedis.popstellar.model.network.answer.Answer;
import com.github.dedis.popstellar.model.network.answer.Error;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

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
    return obj;
  }
}
