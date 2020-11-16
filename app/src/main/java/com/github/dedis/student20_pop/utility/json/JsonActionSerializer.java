package com.github.dedis.student20_pop.utility.json;

import com.github.dedis.student20_pop.model.network.ChanneledMessage;
import com.github.dedis.student20_pop.model.network.Method;
import com.github.dedis.student20_pop.model.network.Request;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class JsonActionSerializer implements JsonSerializer<ChanneledMessage>, JsonDeserializer<ChanneledMessage> {

    @Override
    public ChanneledMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonRPCRequest container = context.deserialize(json, JsonRPCRequest.class);
        if(!container.jsonrpc.equals("2.0"))
            throw new JsonParseException("Unable to parse jsonrpc version : " + container.jsonrpc);

        Method method = Method.find(container.method);
        if(method == null)
            throw new JsonParseException("Unknown method type " + container.method);
        JsonObject params = container.params;

        if(method.expectResult())
            params.add("id", json.getAsJsonObject().get("id"));

        return context.deserialize(params, method.getDataClass());
    }

    @Override
    public JsonElement serialize(ChanneledMessage src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject params = context.serialize(src).getAsJsonObject();

        JsonObject obj = context.serialize(new JsonRPCRequest("2.0", src.getMethod(), params)).getAsJsonObject();

        if(src instanceof Request)
            obj.addProperty("id", ((Request) src).getRequestID());

        return obj;
    }

    private static final class JsonRPCRequest {
        private final String jsonrpc;
        private final String method;
        private final JsonObject params;

        private JsonRPCRequest(String jsonrpc, String method, JsonObject params) {
            this.jsonrpc = jsonrpc;
            this.method = method;
            this.params = params;
        }
    }
}
