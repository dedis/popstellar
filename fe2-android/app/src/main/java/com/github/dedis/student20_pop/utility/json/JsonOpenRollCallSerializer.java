package com.github.dedis.student20_pop.utility.json;

import android.util.Log;

import com.github.dedis.student20_pop.model.network.method.message.data.rollcall.OpenRollCall;
import com.google.gson.*;
import java.lang.reflect.Type;

/** Json serializer and deserializer for the CreateRollCall message */
public class JsonOpenRollCallSerializer
        implements JsonSerializer<OpenRollCall>, JsonDeserializer<OpenRollCall> {


    private final Gson internalGson = new Gson();

    @Override
    public OpenRollCall deserialize(
            JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject object = json.getAsJsonObject();

        Log.d("json", object.toString());
        return new OpenRollCall(
                object.get("update_id").getAsString(),
                object.get("opens").getAsString(),
                object.get("opened_at").getAsLong(),
                object.get("action").getAsString());
    }

    @Override
    public JsonElement serialize(
            OpenRollCall src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();

        result.addProperty("object", src.getObject());
        result.addProperty("action", src.getAction());
        result.addProperty("update_id", src.getUpdateId());
        result.addProperty("opens", src.getOpens());
        result.addProperty("opened_at", src.getOpenedAt());

        return result;
    }
}