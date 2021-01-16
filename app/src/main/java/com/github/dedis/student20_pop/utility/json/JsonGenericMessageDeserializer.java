package com.github.dedis.student20_pop.utility.json;

import com.github.dedis.student20_pop.model.network.GenericMessage;
import com.github.dedis.student20_pop.model.network.answer.Answer;
import com.github.dedis.student20_pop.model.network.method.Message;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class JsonGenericMessageDeserializer implements JsonDeserializer<GenericMessage> {

    private static final String METHOD = "method";

    @Override
    public GenericMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if(json.getAsJsonObject().has(METHOD))
            return context.deserialize(json, Message.class);
        else
            return context.deserialize(json, Answer.class);
    }
}
