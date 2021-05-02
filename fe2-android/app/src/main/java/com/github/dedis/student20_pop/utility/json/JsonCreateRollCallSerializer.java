package com.github.dedis.student20_pop.utility.json;

import android.util.Log;

import com.github.dedis.student20_pop.model.network.method.message.data.rollcall.CreateRollCall;
import com.google.gson.*;
import java.lang.reflect.Type;

/** Json serializer and deserializer for the CreateRollCall message */
public class JsonCreateRollCallSerializer
    implements JsonSerializer<CreateRollCall>, JsonDeserializer<CreateRollCall> {

  private static final String DESCRIPTION = "description";

  private final Gson internalGson = new Gson();

  @Override
  public CreateRollCall deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {

    JsonObject object = json.getAsJsonObject();
    String desc = object.has(DESCRIPTION) ? object.get(DESCRIPTION).getAsString() : null;

    return new CreateRollCall(
        object.get("id").getAsString(),
        object.get("name").getAsString(),
        object.get("creation").getAsLong(),
        object.get("proposed_start").getAsLong(),
        object.get("proposed_end").getAsLong(),
        object.get("location").getAsString(),
        desc);
  }

  @Override
  public JsonElement serialize(
      CreateRollCall src, Type typeOfSrc, JsonSerializationContext context) {
    //JsonObject object = internalGson.toJsonTree(src, CreateRollCall.class).getAsJsonObject();
    JsonObject result = new JsonObject();

    result.addProperty("object", src.getObject());
    result.addProperty("action", src.getAction());
    result.addProperty("id", src.getId());
    result.addProperty("name", src.getName());
    result.addProperty("creation", src.getCreation());
    result.addProperty("proposed_start", src.getProposedStart());
    result.addProperty("proposed_end", src.getProposedEnd());
    result.addProperty("location", src.getLocation());
    // Add optional field if needed
    src.getDescription().ifPresent(desc -> result.addProperty(DESCRIPTION, desc));
    return result;
  }
}
