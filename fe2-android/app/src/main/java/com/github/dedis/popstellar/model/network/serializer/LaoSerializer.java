package com.github.dedis.popstellar.model.network.serializer;

import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.*;

/** This class serializes the LAO in a JSON string to be stored in the database */
public class LaoSerializer implements JsonSerializer<Lao>, JsonDeserializer<Lao> {

  @Override
  public Lao deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();

    // Deserialize the nested Channel object
    Channel channel = context.deserialize(jsonObject.get("channel"), Channel.class);

    // Deserialize the other properties of the Lao object
    String id = jsonObject.get("id").getAsString();
    String name = jsonObject.get("name").getAsString();
    Long lastModified = jsonObject.get("lastModified").getAsLong();
    Long creation = jsonObject.get("creation").getAsLong();
    PublicKey organizer = new PublicKey(jsonObject.get("organizer").getAsString());
    MessageID modificationId =
        context.deserialize(jsonObject.get("modificationId"), MessageID.class);

    // Deserialize the Set of Witnesses
    JsonArray witnessesJsonArray = jsonObject.get("witnesses").getAsJsonArray();
    Set<PublicKey> witnesses = new HashSet<>();
    for (JsonElement witnessJsonElement : witnessesJsonArray) {
      witnesses.add(new PublicKey(witnessJsonElement.getAsString()));
    }

    // Deserialize the Map of witnessMessages
    JsonObject witnessMessagesJsonObject = jsonObject.get("witnessMessages").getAsJsonObject();
    Map<MessageID, WitnessMessage> witnessMessages = new HashMap<>();
    for (Map.Entry<String, JsonElement> entry : witnessMessagesJsonObject.entrySet()) {
      witnessMessages.put(
          new MessageID(entry.getKey()),
          context.deserialize(entry.getValue(), WitnessMessage.class));
    }

    return new LaoBuilder()
        .setChannel(channel)
        .setId(id)
        .setName(name)
        .setLastModified(lastModified)
        .setCreation(creation)
        .setOrganizer(organizer)
        .setModificationId(modificationId)
        .setWitnesses(witnesses)
        .setWitnessMessages(witnessMessages)
        .build();
  }

  @Override
  public JsonElement serialize(Lao lao, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject jsonObject = new JsonObject();

    // Serialize the nested Channel object
    jsonObject.add("channel", context.serialize(lao.getChannel(), Channel.class));

    // Serialize the other properties of the Lao object
    jsonObject.addProperty("id", lao.getId());
    jsonObject.addProperty("name", lao.getName());
    jsonObject.addProperty("lastModified", lao.getLastModified());
    jsonObject.addProperty("creation", lao.getCreation());
    jsonObject.addProperty("organizer", lao.getOrganizer().getEncoded());

    jsonObject.add("modificationId", context.serialize(lao.getModificationId(), MessageID.class));

    // Serialize the Set of Witnesses
    JsonArray witnessesJsonArray = new JsonArray();
    for (PublicKey witness : lao.getWitnesses()) {
      witnessesJsonArray.add(context.serialize(witness, WitnessMessage.class));
    }
    jsonObject.add("witnesses", witnessesJsonArray);

    // Serialize the Map of witnessMessages
    JsonObject witnessMessagesJsonObject = new JsonObject();
    for (Map.Entry<MessageID, WitnessMessage> entry : lao.getWitnessMessages().entrySet()) {
      witnessMessagesJsonObject.add(
          entry.getKey().getEncoded(), context.serialize(entry.getValue(), WitnessMessage.class));
    }
    jsonObject.add("witnessMessages", witnessMessagesJsonObject);

    return jsonObject;
  }
}
