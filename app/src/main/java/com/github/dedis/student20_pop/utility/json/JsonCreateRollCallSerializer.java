package com.github.dedis.student20_pop.utility.json;

import com.github.dedis.student20_pop.model.network.query.data.rollcall.CreateRollCall;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class JsonCreateRollCallSerializer implements JsonSerializer<CreateRollCall>, JsonDeserializer<CreateRollCall> {

    private static final String DESCRIPTION = "roll_call_description";

    private final Gson internalGson = new Gson();

    @Override
    public CreateRollCall deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        CreateRollCall temp = internalGson.fromJson(json, CreateRollCall.class);

        JsonObject object = json.getAsJsonObject();
        String desc = object.has(DESCRIPTION) ? object.get(DESCRIPTION).getAsString() : null;

        for (CreateRollCall.StartType type : CreateRollCall.StartType.ALL) {
            if (object.has(type.getJsonMember())) {
                long start = object.get(type.getJsonMember()).getAsLong();
                return new CreateRollCall(temp.getId(), temp.getName(), temp.getCreation(), start, type, temp.getLocation(), desc);
            }
        }

        throw new JsonParseException("Could not find start time");
    }

    @Override
    public JsonElement serialize(CreateRollCall src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = internalGson.toJsonTree(src, CreateRollCall.class).getAsJsonObject();
        // Add optional field if needed
        src.getDescription().ifPresent(desc -> object.addProperty(DESCRIPTION, desc));
        // Add start time with the good field
        object.addProperty(src.getStartType().getJsonMember(), src.getStartTime());
        return object;
    }
}
