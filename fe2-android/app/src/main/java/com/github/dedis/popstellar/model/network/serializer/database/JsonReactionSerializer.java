package com.github.dedis.popstellar.model.network.serializer.database;

import com.github.dedis.popstellar.model.objects.Reaction;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.google.gson.*;

import java.lang.reflect.Type;

public class JsonReactionSerializer
    implements JsonSerializer<Reaction>, JsonDeserializer<Reaction> {

  @Override
  public Reaction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();

    MessageID id = new MessageID(jsonObject.get("id").getAsString());
    PublicKey sender = new PublicKey(jsonObject.get("sender").getAsString());
    String codepoint = jsonObject.get("codepoint").getAsString();
    MessageID chirpId = new MessageID(jsonObject.get("chirpId").getAsString());
    long timestamp = jsonObject.get("timestamp").getAsLong();
    boolean isDeleted = jsonObject.get("isDeleted").getAsBoolean();

    return new Reaction(id, sender, codepoint, chirpId, timestamp, isDeleted);
  }

  @Override
  public JsonElement serialize(Reaction src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject jsonObject = new JsonObject();

    jsonObject.addProperty("id", src.getId().getEncoded());
    jsonObject.addProperty("sender", src.getSender().getEncoded());
    jsonObject.addProperty("codepoint", src.getCodepoint());
    jsonObject.addProperty("chirpId", src.getChirpId().getEncoded());
    jsonObject.addProperty("timestamp", src.getTimestamp());
    jsonObject.addProperty("isDeleted", src.isDeleted());

    return jsonObject;
  }
}
