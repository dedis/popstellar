package com.github.dedis.student20_pop.utility.json;

import com.github.dedis.student20_pop.model.network.level.high.Action;
import com.github.dedis.student20_pop.model.network.level.high.Message;
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

public class JsonMessageSerializer implements JsonSerializer<Message>, JsonDeserializer<Message> {

    @Override
    public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        Objects object = Objects.find(obj.get("object").getAsString());
        Action action = Action.find(obj.get("action").getAsString());

        if(object == null)
            throw new JsonParseException("Unknown object type : " + obj.get("object").getAsString());
        if(action == null)
            throw new JsonParseException("Unknown action type : " + obj.get("action").getAsString());

        Map<Action, Class<? extends Message>> actionClassMap = Message.messages.get(object);
        if(actionClassMap == null)
            throw new JsonParseException("Unknown object type : " + obj.get("object").getAsString());

        Class<? extends Message> clazz = actionClassMap.get(action);
        if(clazz == null)
            throw new JsonParseException("The pair " + object.getObject() + "/" + action.getAction() + " does not exists");

        return context.deserialize(json, clazz);
    }

    @Override
    public JsonElement serialize(Message src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = context.serialize(src).getAsJsonObject();
        obj.addProperty("object", src.getObject());
        obj.addProperty("action", src.getAction());
        return obj;
    }
}
