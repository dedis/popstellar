package com.github.dedis.student20_pop.utility.json;

import com.github.dedis.student20_pop.model.network.method.message.data.rollcall.CloseRollCall;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/** Json serializer and deserializer for the CreateRollCall message */
public class JsonCloseRollCallSerializer
        implements JsonSerializer<CloseRollCall>, JsonDeserializer<CloseRollCall> {


    private final Gson internalGson = new Gson();

    @Override
    public CloseRollCall deserialize(
            JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        List<String> attendees = internalGson.fromJson(object.get("attendees"), new TypeToken<List<String>>(){}.getType());

        return new CloseRollCall(
                object.get("update_id").getAsString(),
                object.get("closes").getAsString(),
                object.get("closed_at").getAsLong(),
                attendees);
    }

    @Override
    public JsonElement serialize(
            CloseRollCall src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();

        result.addProperty("object", src.getObject());
        result.addProperty("action", src.getAction());
        result.addProperty("update_id", src.getUpdateId());
        result.addProperty("closes", src.getCloses());
        result.addProperty("closed_at", src.getClosedAt());
        result.add("attendees", context.serialize(src.getAttendees()));

        return result;
    }
}