package com.github.dedis.popstellar.model.network.serializer.database;

import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class JsonWitnessMessageSerializer
    implements JsonSerializer<WitnessMessage>, JsonDeserializer<WitnessMessage> {

  @Override
  public WitnessMessage deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();

    String title = jsonObject.get("title").getAsString();
    String description = jsonObject.get("description").getAsString();

    MessageID messageID = new MessageID(jsonObject.get("messageId").getAsString());

    Set<PublicKey> witnesses = new HashSet<>();
    JsonArray witnessesArray = jsonObject.getAsJsonArray("witnesses");
    for (JsonElement witness : witnessesArray) {
      PublicKey publicKey = new PublicKey(witness.getAsString());
      witnesses.add(publicKey);
    }

    return new WitnessMessage(messageID, witnesses, title, description);
  }

  @Override
  public JsonElement serialize(
      WitnessMessage message, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject jsonObject = new JsonObject();

    jsonObject.addProperty("title", message.getTitle());
    jsonObject.addProperty("description", message.getDescription());

    jsonObject.addProperty("messageId", message.getMessageId().getEncoded());

    JsonArray witnessesArray = new JsonArray();
    for (PublicKey witness : message.getWitnesses()) {
      String publicKeyString = witness.getEncoded();
      witnessesArray.add(publicKeyString);
    }
    jsonObject.add("witnesses", witnessesArray);

    return jsonObject;
  }
}
