package com.github.dedis.student20_pop.utility.json;

import android.util.Log;
import com.github.dedis.student20_pop.model.network.answer.Result;
import com.github.dedis.student20_pop.model.network.method.message.MessageGeneral;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.List;

public class JsonResultSerializer implements JsonSerializer<Result>, JsonDeserializer<Result> {
  private static final String TAG = JsonResultSerializer.class.getSimpleName();

  @Override
  public Result deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    Log.d(TAG, "Trying to deserialize result");
    JsonObject root = json.getAsJsonObject();

    int id = root.get("id").getAsInt();
    Result result = new Result(id);

    JsonElement resultElement = root.get("result");
    if (resultElement.isJsonPrimitive()) {
      result.setGeneral();
    } else {
      Log.d(TAG, "got a list of messages to deserialize");
      List<MessageGeneral> messages = context.deserialize(resultElement, MessageGeneral.class);
      result.setMessages(messages);
    }

    return result;
  }

  @Override
  public JsonElement serialize(Result src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject output = new JsonObject();

    output.addProperty("id", src.getId());
    if (src.getGeneral().isPresent()) {
      output.addProperty("result", 0);
    } else {
      JsonElement messages = context.serialize(src.getMessages().get());
      output.add("result", messages);
    }

    return output;
  }
}
