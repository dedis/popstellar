package com.github.dedis.popstellar.model.network.serializer;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.network.method.message.data.election.CastVote;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionEncryptedVote;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVote;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Optional;

/** Json serializer and deserializer for the data messages */
public class JsonDataSerializer implements JsonSerializer<Data>, JsonDeserializer<Data> {

  private static final String OBJECT = "object";
  private static final String ACTION = "action";

  private final DataRegistry dataRegistry;

  public JsonDataSerializer(DataRegistry dataRegistry) {
    this.dataRegistry = dataRegistry;
  }

  @Override
  public Data deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject obj = json.getAsJsonObject();
    JsonUtils.verifyJson(JsonUtils.DATA_SCHEMA, obj.toString());
    Objects object = Objects.find(obj.get(OBJECT).getAsString());
    Action action = Action.find(obj.get(ACTION).getAsString());

    if (object == null) {
      throw new JsonParseException("Unknown object type : " + obj.get(OBJECT).getAsString());
    }
    if (action == null) {
      throw new JsonParseException("Unknown action type : " + obj.get(ACTION).getAsString());
    }

    Optional<Class<? extends Data>> clazz = dataRegistry.getType(object, action);
    if (!clazz.isPresent()) {
      throw new JsonParseException(
          "The pair ("
              + object.getObject()
              + ", "
              + action.getAction()
              + ") does not exists in the protocol");
    }
    // If action is a CastVote, we need to create a custom deserializer
    if (action == Action.CAST_VOTE) {
      return castVoteDeserializer(json, context);
    }
    return context.deserialize(json, clazz.get());
  }

  @Override
  public JsonElement serialize(Data src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject obj = context.serialize(src).getAsJsonObject();
    obj.addProperty(OBJECT, src.getObject());
    obj.addProperty(ACTION, src.getAction());
    JsonUtils.verifyJson(JsonUtils.DATA_SCHEMA, obj.toString());
    return obj;
  }

  public CastVote castVoteDeserializer(JsonElement json, JsonDeserializationContext context) {
    JsonObject obj = json.getAsJsonObject();
    JsonArray jsonVote = obj.getAsJsonArray("votes");
    boolean typeValidationInt = true;
    boolean typeValidationString = true;
    // Vote type of a CastVote is either an integer for an OpenBallot election or a
    // String for an Encrypted election, type should be valid for all votes
    for (int i = 0; i < jsonVote.size(); i++) {
      JsonObject voteContent = jsonVote.get(i).getAsJsonObject();
      typeValidationInt =
          typeValidationInt && voteContent.get("vote").getAsJsonPrimitive().isNumber();
      typeValidationString =
          typeValidationString && voteContent.get("vote").getAsJsonPrimitive().isString();
    }
    if (typeValidationInt && !typeValidationString) {
      Type token = new TypeToken<CastVote<ElectionVote>>() {}.getType();
      return context.deserialize(json, token);
    } else if (!typeValidationInt && typeValidationString) {
      Type token = new TypeToken<CastVote<ElectionEncryptedVote>>() {}.getType();
      return context.deserialize(json, token);
    } else {
      throw new JsonParseException("Unknown vote type in cast vote message");
    }
  }


}
