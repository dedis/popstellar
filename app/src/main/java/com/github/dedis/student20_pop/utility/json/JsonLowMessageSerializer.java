package com.github.dedis.student20_pop.utility.json;

import com.github.dedis.student20_pop.model.network.level.low.ChanneledMessage;
import com.github.dedis.student20_pop.model.network.level.low.Method;
import com.github.dedis.student20_pop.model.network.level.low.Request;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Json serializer and deserializer for the low level messages
 */
public class JsonLowMessageSerializer implements JsonSerializer<ChanneledMessage>, JsonDeserializer<ChanneledMessage> {

    @Override
    public ChanneledMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonRPCRequest container = context.deserialize(json, JsonRPCRequest.class);
        JsonUtils.testRPCVersion(container.jsonrpc);

        Method method = Method.find(container.method);
        if(method == null)
            throw new JsonParseException("Unknown method type " + container.method);
        JsonObject params = container.params;

        // If the Channeled Message is a Request, we need to give the params the id the the request
        if(method.expectResult())
            params.add(JsonUtils.JSON_REQUEST_ID, json.getAsJsonObject().get(JsonUtils.JSON_REQUEST_ID));

        return context.deserialize(params, method.getDataClass());
    }

    @Override
    public JsonElement serialize(ChanneledMessage src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject params = context.serialize(src).getAsJsonObject();

        JsonObject obj = context.serialize(new JsonRPCRequest(JsonUtils.JSON_RPC_VERSION, src.getMethod(), params)).getAsJsonObject();

        if(src instanceof Request)
            obj.addProperty(JsonUtils.JSON_REQUEST_ID, ((Request) src).getRequestID());

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
