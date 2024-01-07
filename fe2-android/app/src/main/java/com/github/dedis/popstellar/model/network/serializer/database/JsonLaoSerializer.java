package com.github.dedis.popstellar.model.network.serializer.database;

import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.*;

/** This class serializes the LAO in a JSON string to be stored in the database */
public class JsonLaoSerializer implements JsonSerializer<Lao>, JsonDeserializer<Lao> {

  @Override
  public Lao deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();

    // Deserialize the nested Channel object
    Channel channel = context.deserialize(jsonObject.get("channel"), Channel.class);

    // Deserialize the other properties of the Lao object
    String id = jsonObject.get("id").getAsString();
    String name = jsonObject.get("name").getAsString();
    // LastModified could be absent
    JsonElement lastModifiedElement = jsonObject.get("lastModified");
    Long lastModified = lastModifiedElement == null ? null : lastModifiedElement.getAsLong();
    Long creation = jsonObject.get("creation").getAsLong();

    PublicKey organizer = context.deserialize(jsonObject.get("organizer"), PublicKey.class);
    MessageID modificationId =
        context.deserialize(jsonObject.get("modificationId"), MessageID.class);

    // Deserialize the Set of PendingUpdate
    JsonArray pendingUpdatesJsonArray = jsonObject.get("pendingUpdates").getAsJsonArray();
    Set<PendingUpdate> pendingUpdates = new HashSet<>();
    for (JsonElement pendingUpdatesJsonElement : pendingUpdatesJsonArray) {
      pendingUpdates.add(context.deserialize(pendingUpdatesJsonElement, PendingUpdate.class));
    }

    return new LaoBuilder()
        .setChannel(channel)
        .setId(id)
        .setName(name)
        .setLastModified(lastModified)
        .setCreation(creation)
        .setOrganizer(organizer)
        .setModificationId(modificationId)
        .setPendingUpdates(pendingUpdates)
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

    jsonObject.add("organizer", context.serialize(lao.getOrganizer(), PublicKey.class));
    jsonObject.add("modificationId", context.serialize(lao.getModificationId(), MessageID.class));

    // Serialize the Set of PendingUpdate
    JsonArray pendingUpdateJsonArray = new JsonArray();
    for (PendingUpdate pendingUpdate : lao.getPendingUpdates()) {
      pendingUpdateJsonArray.add(context.serialize(pendingUpdate, PendingUpdate.class));
    }
    jsonObject.add("pendingUpdates", pendingUpdateJsonArray);

    return jsonObject;
  }
}
