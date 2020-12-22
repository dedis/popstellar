package com.github.dedis.student20_pop.utility.json;

import com.github.dedis.student20_pop.model.network.level.high.Action;
import com.github.dedis.student20_pop.model.network.level.high.Data;
import com.github.dedis.student20_pop.model.network.level.high.Objects;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Json serializer and deserializer for the high level messages
 */
public class JsonMessageSerializer implements JsonSerializer<Data>, JsonDeserializer<Data> {

    private static final String OBJECT = "object";
    private static final String ACTION = "action";

    @Override
    public Data deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        Objects object = Objects.find(obj.get(OBJECT).getAsString());
        Action action = Action.find(obj.get(ACTION).getAsString());

        if (object == null)
            throw new JsonParseException("Unknown object type : " + obj.get(OBJECT).getAsString());
        if (action == null)
            throw new JsonParseException("Unknown action type : " + obj.get(ACTION).getAsString());

        Map<Action, Class<? extends Data>> actionClassMap = Data.messages.get(object);
        if (actionClassMap == null)
            throw new JsonParseException("Unknown object type : " + obj.get(OBJECT).getAsString());

        Class<? extends Data> clazz = actionClassMap.get(action);
        if (clazz == null)
            throw new JsonParseException("The pair " + object.getObject() + "/" + action.getAction() + " does not exists");

        return context.deserialize(json, clazz);
    }

    @Override
    public JsonElement serialize(Data src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = context.serialize(src).getAsJsonObject();
        obj.addProperty(OBJECT, src.getObject());
        obj.addProperty(ACTION, src.getAction());
        return obj;
    }
}
