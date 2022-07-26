package com.github.dedis.popstellar.model.network.serializer;

import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.answer.ResultMessages;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class JsonResultSerializer implements JsonSerializer<Result>, JsonDeserializer<Result> {

  private static final String TAG = JsonResultSerializer.class.getSimpleName();
  private final String RESULT = "result";
  private final String ID = "ID";

  @Override
  public Result deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject root = json.getAsJsonObject();

    int id = root.get("id").getAsInt();

    JsonElement resultElement = root.get(RESULT);
    if (resultElement.isJsonPrimitive()) {
      return new Result(id);
    } else {
      Type listType = new TypeToken<ArrayList<MessageGeneral>>() {}.getType();
      List<MessageGeneral> messages = context.deserialize(resultElement.getAsJsonArray(), listType);
      return new ResultMessages(id, messages);
    }
  }

  @Override
  public JsonElement serialize(Result src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject output = new JsonObject();

    output.addProperty(ID, src.getId());
    if (src instanceof ResultMessages) {
      ResultMessages resultMessages = (ResultMessages) src;
      JsonElement messages = context.serialize(resultMessages.getMessages());
      output.add(RESULT, messages);
    } else {
      output.addProperty(RESULT, 0);
    }

    return output;
  }
}
