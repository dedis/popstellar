package com.github.dedis.student20_pop.utility.json;
import com.github.dedis.student20_pop.model.network.method.message.data.election.ElectionQuestion;
import com.github.dedis.student20_pop.model.network.method.message.data.election.ElectionCastVotes;
import com.github.dedis.student20_pop.model.network.method.message.data.election.ElectionSetup;
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
import java.util.ArrayList;
import java.util.List;
public class JsonCastingVotesSerializer implements JsonSerializer<ElectionCastVotes>, JsonDeserializer<ElectionCastVotes> {

    private final Gson internalGson = new Gson();

    @Override
    public ElectionCastVotes deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ElectionCastVotes temp = internalGson.fromJson(json, ElectionCastVotes.class);

        //Deserialize vote ?
        JsonElectionVotesSerializer serializer = new JsonElectionVotesSerializer();

        JsonObject object = json.getAsJsonObject();
        // Serialize/Deserialize questions ?

        //TODO: iteratively add questions when implementing multiple questions
        ElectionVote vote = temp.getVotes().get(0);
        List<List<Long>> votes = new ArrayList<>();
        votes.add(vote.getVote_results());

        return new ElectionCastVotes(
                vote.getWriteIn(),
                votes,
                vote.getQuestionId(),
                temp.getElectionId(),
                temp.getLaoId()
        );
    }

    @Override
    public JsonElement serialize(ElectionCastVotes src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = internalGson.toJsonTree(src, ElectionSetup.class).getAsJsonObject();
        return object;
    }
}
