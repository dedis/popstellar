package com.github.dedis.popstellar.model.network.serializer.database;

import com.github.dedis.popstellar.model.objects.Meeting;
import com.github.dedis.popstellar.model.objects.event.MeetingBuilder;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class JsonMeetingSerializer implements JsonSerializer<Meeting>, JsonDeserializer<Meeting> {

  @Override
  public Meeting deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();

    String id = jsonObject.get("id").getAsString();
    String name = jsonObject.get("name").getAsString();
    long creation = jsonObject.get("creation").getAsLong();
    long start = jsonObject.get("start").getAsLong();
    long end = jsonObject.get("end").getAsLong();
    String location = jsonObject.get("location").getAsString();
    long lastModified = jsonObject.get("lastModified").getAsLong();
    String modificationId = jsonObject.get("modificationId").getAsString();

    List<String> modificationSignatures = new ArrayList<>();
    JsonArray modificationSignaturesArray = jsonObject.getAsJsonArray("modificationSignatures");
    for (JsonElement modificationSignature : modificationSignaturesArray) {
      modificationSignatures.add(modificationSignature.getAsString());
    }

    return new MeetingBuilder()
        .setId(id)
        .setName(name)
        .setCreation(creation)
        .setStart(start)
        .setEnd(end)
        .setLocation(location)
        .setLastModified(lastModified)
        .setModificationId(modificationId)
        .setModificationSignatures(modificationSignatures)
        .build();
  }

  @Override
  public JsonElement serialize(Meeting src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject jsonObject = new JsonObject();

    jsonObject.addProperty("id", src.getId());
    jsonObject.addProperty("name", src.getName());
    jsonObject.addProperty("creation", src.getCreation());
    jsonObject.addProperty("start", src.getStartTimestamp());
    jsonObject.addProperty("end", src.getEndTimestamp());
    jsonObject.addProperty("location", src.getLocation());
    jsonObject.addProperty("lastModified", src.getLastModified());
    jsonObject.addProperty("modificationId", src.getModificationId());

    JsonArray modificationSignaturesArray = new JsonArray();
    for (String modificationSignature : src.getModificationSignatures()) {
      modificationSignaturesArray.add(modificationSignature);
    }
    jsonObject.add("modificationSignatures", modificationSignaturesArray);

    return jsonObject;
  }
}
