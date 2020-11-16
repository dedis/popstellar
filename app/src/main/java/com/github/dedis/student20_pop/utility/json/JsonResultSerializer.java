package com.github.dedis.student20_pop.utility.json;

import com.github.dedis.student20_pop.model.network.result.Failure;
import com.github.dedis.student20_pop.model.network.result.Result;
import com.github.dedis.student20_pop.model.network.result.Success;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class JsonResultSerializer implements JsonSerializer<Result>, JsonDeserializer<Result> {

    @Override
    public Result deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        if(!obj.has("jsonrpc") || !obj.get("jsonrpc").getAsString().equals("2.0"))
            throw new JsonParseException("Unable to parse jsonrpc version : " + obj.get("jsonrpc").getAsString());

        if(obj.has("result"))
            return context.deserialize(json, Success.class);
        else if(obj.has("error"))
            return context.deserialize(json, Failure.class);
        else
            throw new JsonParseException("A result must contain one of the field result or error");
    }

    @Override
    public JsonElement serialize(Result src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = context.serialize(src).getAsJsonObject();
        obj.addProperty("jsonrpc", "2.0");
        return obj;
    }
}
