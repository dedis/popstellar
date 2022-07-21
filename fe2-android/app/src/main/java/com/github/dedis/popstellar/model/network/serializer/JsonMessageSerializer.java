package com.github.dedis.popstellar.model.network.serializer;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.*;
import com.google.gson.*;

import java.lang.reflect.Type;

/** Json serializer and deserializer for the low level messages */
public class JsonMessageSerializer implements JsonSerializer<Message>, JsonDeserializer<Message> {

  @Override
  public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    Log.d("deserializer", "deserializing message");
    JSONRPCRequest container = context.deserialize(json, JSONRPCRequest.class);
    JsonUtils.testRPCVersion(container.getJsonrpc());

    Method method = Method.find(container.getMethod());
    if (method == null) {
      throw new JsonParseException("Unknown method type " + container.getMethod());
    }
    JsonObject params = container.getParams();

    // If the Channeled Data is a Query, we need to give the params the id the the request
    if (method.expectResult()) {
      params.add(JsonUtils.JSON_REQUEST_ID, json.getAsJsonObject().get(JsonUtils.JSON_REQUEST_ID));
    }

    return context.deserialize(params, method.getDataClass());
  }

  @Override
  public JsonElement serialize(Message src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject params = context.serialize(src).getAsJsonObject();

    JsonObject obj =
        context
            .serialize(new JSONRPCRequest(JsonUtils.JSON_RPC_VERSION, src.getMethod(), params))
            .getAsJsonObject();

    if (src instanceof Query) {
      obj.addProperty(JsonUtils.JSON_REQUEST_ID, ((Query) src).getRequestId());
    }

    return obj;
  }
}
