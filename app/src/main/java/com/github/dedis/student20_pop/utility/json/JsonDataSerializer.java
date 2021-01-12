package com.github.dedis.student20_pop.utility.json;

import com.github.dedis.student20_pop.model.network.method.message.data.Action;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.Objects;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Json serializer and deserializer for the data messages
 */
public class JsonDataSerializer implements JsonSerializer<Data>, JsonDeserializer<Data> {

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

        Optional<Class<? extends Data>> clazz = Data.getType(object, action);
        if (!clazz.isPresent())
            throw new JsonParseException("The pair (" + object.getObject() + ", " + action.getAction() + ") does not exists in the protocol");

        return context.deserialize(json, clazz.get());
    }

    @Override
    public JsonElement serialize(Data src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = context.serialize(src).getAsJsonObject();
        obj.addProperty(OBJECT, src.getObject());
        obj.addProperty(ACTION, src.getAction());
        return obj;
    }
}
