package com.github.dedis.popstellar.model.network.serializer.database;

import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.google.gson.*;

import java.lang.reflect.Type;

public class JsonChirpSerializer implements JsonSerializer<Chirp>, JsonDeserializer<Chirp> {

  @Override
  public Chirp deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();

    MessageID id = new MessageID(jsonObject.get("id").getAsString());
    PublicKey sender = new PublicKey(jsonObject.get("sender").getAsString());
    String text = jsonObject.get("text").getAsString();
    long timestamp = jsonObject.get("timestamp").getAsLong();
    boolean isDeleted = jsonObject.get("isDeleted").getAsBoolean();
    MessageID parentId = new MessageID(jsonObject.get("parentId").getAsString());

    return new Chirp(id, sender, text, timestamp, isDeleted, parentId);
  }

  @Override
  public JsonElement serialize(Chirp src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject jsonObject = new JsonObject();

    jsonObject.addProperty("id", src.getId().getEncoded());
    jsonObject.addProperty("sender", src.getSender().getEncoded());
    jsonObject.addProperty("text", src.getText());
    jsonObject.addProperty("timestamp", src.getTimestamp());
    jsonObject.addProperty("isDeleted", src.isDeleted());
    jsonObject.addProperty("parentId", src.getParentId().getEncoded());

    return jsonObject;
  }
}
