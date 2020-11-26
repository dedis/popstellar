package com.github.dedis.student20_pop.utility.json;

import com.github.dedis.student20_pop.model.network.level.low.result.Failure;
import com.github.dedis.student20_pop.model.network.level.low.result.Result;
import com.github.dedis.student20_pop.model.network.level.low.result.Success;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class JsonResultSerializer implements JsonSerializer<Result>, JsonDeserializer<Result> {

    private static final String RESULT = "result";
    private static final String ERROR = "error";

    @Override
    public Result deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        JsonUtils.testRPCVersion(obj);

        if(obj.has(RESULT))
            return context.deserialize(json, Success.class);
        else if(obj.has(ERROR))
            return context.deserialize(json, Failure.class);
        else
            throw new JsonParseException("A result must contain one of the field result or error");
    }

    @Override
    public JsonElement serialize(Result src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = context.serialize(src).getAsJsonObject();
        obj.addProperty(JsonUtils.JSON_RPC, JsonUtils.JSON_RPC_VERSION);
        return obj;
    }
}
