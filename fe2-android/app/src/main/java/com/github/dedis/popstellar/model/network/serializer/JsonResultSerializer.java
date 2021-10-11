package com.github.dedis.popstellar.model.network.serializer;

import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class JsonResultSerializer implements JsonSerializer<Result>, JsonDeserializer<Result> {

  private static final String TAG = JsonResultSerializer.class.getSimpleName();

  @Override
  public Result deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject root = json.getAsJsonObject();

    int id = root.get("id").getAsInt();
    Result result = new Result(id);

    JsonElement resultElement = root.get("result");
    if (resultElement.isJsonPrimitive()) {
      result.setGeneral();
    } else {
      Type listType = new TypeToken<ArrayList<MessageGeneral>>() {}.getType();
      List<MessageGeneral> messages = context.deserialize(resultElement.getAsJsonArray(), listType);
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
