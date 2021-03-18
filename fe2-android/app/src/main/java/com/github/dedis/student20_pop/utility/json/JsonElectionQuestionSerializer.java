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

public class JsonElectionQuestionSerializer implements JsonSerializer<ElectionQuestion>, JsonDeserializer<ElectionQuestion> {

    private final Gson internalGson = new Gson();

    @Override
    public ElectionQuestion deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ElectionQuestion temp = internalGson.fromJson(json, ElectionQuestion.class);
        return new ElectionQuestion(
                temp.getQuestion(),
                temp.getVotingMethod(),
                temp.getWriteIn(),
                temp.getBallotOptions(),
                temp.getId()
        );
    }

    @Override
    public JsonElement serialize(ElectionQuestion src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = internalGson.toJsonTree(src, ElectionQuestion.class).getAsJsonObject();
        return object;
    }
}
