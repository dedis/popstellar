package com.github.dedis.student20_pop.utility.json;

import com.github.dedis.student20_pop.model.network.method.message.data.election.ElectionQuestion;
import com.github.dedis.student20_pop.model.network.method.message.data.election.ElectionSetup;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class JsonElectionSetupSerializer implements JsonSerializer<ElectionSetup>, JsonDeserializer<ElectionSetup> {


    private final Gson internalGson = new Gson();

    @Override
    public ElectionSetup deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ElectionSetup temp = internalGson.fromJson(json, ElectionSetup.class);

        ElectionQuestion question = temp.getQuestion();

        return new ElectionSetup(
                temp.getName(),
                temp.getStartTime(),
                temp.getEndTime(),
                question.getVotingMethod(),
                question.getWriteIn(),
                question.getBallotOptions(),
                question.getQuestion(),
                temp.getId()
        );
    }

    @Override
    public JsonElement serialize(ElectionSetup src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject object = internalGson.toJsonTree(src, ElectionSetup.class).getAsJsonObject();
            return object;
    }
}
