package com.github.dedis.student20_pop.utility.json;
import com.github.dedis.student20_pop.model.network.method.message.data.election.ElectionVote;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class JsonElectionVotesSerializer implements JsonSerializer<ElectionVote>, JsonDeserializer<ElectionVote> {
    private final Gson internalGson = new Gson();

    @Override
    public ElectionVote deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ElectionVote temp = internalGson.fromJson(json, ElectionVote.class);
        return new ElectionVote(
                temp.getQuestionId(),
                temp.getVote_results(),
                temp.getWriteIn(),
                temp.getId()
        );
    }

    @Override
    public JsonElement serialize(ElectionVote src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = internalGson.toJsonTree(src, ElectionVote.class).getAsJsonObject();
        return object;
    }
}
