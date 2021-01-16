package com.github.dedis.student20_pop.utility.json;

import com.github.dedis.student20_pop.model.network.GenericMessage;
import com.github.dedis.student20_pop.model.network.answer.Answer;
import com.github.dedis.student20_pop.model.network.method.Message;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.rollcall.CreateRollCall;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public final class JsonUtils {

    public static final String JSON_RPC = "jsonrpc";
    public static final String JSON_RPC_VERSION = "2.0";

    public static final String JSON_REQUEST_ID = "id";

    private JsonUtils() {
    }

    public static void testRPCVersion(JsonObject object) throws JsonParseException {
        if (!object.has(JSON_RPC))
            throw new JsonParseException("Unable to find jsonrpc version");

        testRPCVersion(object.get(JSON_RPC).getAsString());
    }

    public static void testRPCVersion(String version) throws JsonParseException {
        if (!version.equals(JSON_RPC_VERSION))
            throw new JsonParseException("Unable to parse jsonrpc version : " + version);
    }

    public static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(GenericMessage.class, new JsonGenericMessageDeserializer())
                .registerTypeAdapter(Message.class, new JsonMessageSerializer())
                .registerTypeAdapter(Data.class, new JsonDataSerializer())
                .registerTypeAdapter(Answer.class, new JsonAnswerSerializer())
                .registerTypeAdapter(CreateRollCall.class, new JsonCreateRollCallSerializer())
                .create();
    }
}
