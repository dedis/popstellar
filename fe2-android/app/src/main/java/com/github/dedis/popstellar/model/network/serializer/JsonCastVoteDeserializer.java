package com.github.dedis.popstellar.model.network.serializer;


import com.github.dedis.popstellar.model.network.method.message.data.election.CastVote;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionEncryptedVote;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVote;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class JsonCastVoteDeserializer implements JsonDeserializer<CastVote> {

    private static String voteIndex = "vote";

    @Override
    public CastVote deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        JsonArray jsonVote = obj.getAsJsonArray("votes");

        // Parse fields of the Json
        JsonElement electionIdField = json.getAsJsonObject().get("election");
        JsonElement laoIdField = json.getAsJsonObject().get("lao");
        JsonElement createdAtField = json.getAsJsonObject().get("created_at");

        // Get content
        String electionId = context.deserialize(electionIdField, String.class);
        String laoId = context.deserialize(laoIdField, String.class);
        long createdAt = context.deserialize(createdAtField, long.class);

        boolean typeValidationInt = true;
        boolean typeValidationString = true;
        // Vote type of a CastVote is either an integer for an OpenBallot election or a
        // String for an Encrypted election, type should be valid for all votes
        for (int i = 0; i < jsonVote.size(); i++) {
            JsonObject voteContent = jsonVote.get(i).getAsJsonObject();
            typeValidationInt =
                    typeValidationInt && voteContent.get(voteIndex).getAsJsonPrimitive().isNumber();
            typeValidationString =
                    typeValidationString && voteContent.get(voteIndex).getAsJsonPrimitive().isString();
        }
        if (typeValidationInt && !typeValidationString) {
            Type token = new TypeToken<List<ElectionVote>>() {}.getType();
            List<ElectionVote> votes = context.deserialize(jsonVote, token);
            return new CastVote(votes, electionId, laoId, createdAt);
        } else if (!typeValidationInt && typeValidationString) {
            Type token = new TypeToken<List<ElectionEncryptedVote>>() {}.getType();
            List<ElectionEncryptedVote> votes = context.deserialize(jsonVote, token);
            return new CastVote(votes, electionId, laoId, createdAt);
        } else {
            throw new JsonParseException("Unknown vote type in cast vote message");
        }
    }
}
