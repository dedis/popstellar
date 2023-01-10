package com.github.dedis.popstellar.model.network.serializer;

import com.github.dedis.popstellar.model.network.method.message.data.election.*;
import com.google.gson.*;

import java.lang.reflect.Type;

/** Simple serializer to convert json elements to Vote types and vice-versa */
public class JsonVoteSerializer implements JsonSerializer<Vote>, JsonDeserializer<Vote> {

  @Override
  public JsonElement serialize(Vote src, Type typeOfSrc, JsonSerializationContext context) {
    // make sure the actual class of the vote is used for serialization.
    // Even if the asked type is Vote
    return context.serialize(src);
  }

  @Override
  public Vote deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    IntermediateVote inter = context.deserialize(json, IntermediateVote.class);
    return inter.convert();
  }

  @SuppressWarnings("unused")
  private static class IntermediateVote {
    private String id;
    private String question;
    private JsonPrimitive vote;

    public Vote convert() {
      if (vote.isString()) {
        return new EncryptedVote(id, question, vote.getAsString());
      } else if (vote.isNumber()) {
        return new PlainVote(id, question, vote.getAsInt());
      } else {
        throw new JsonParseException("Unknown vote type");
      }
    }
  }
}
