package com.github.dedis.popstellar.model.network.serializer;

import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.event.RollCallBuilder;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class JsonRollCallSerializer
    implements JsonSerializer<RollCall>, JsonDeserializer<RollCall> {

  @Override
  public RollCall deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();

    String id = jsonObject.get("id").getAsString();
    String persistentId = jsonObject.get("persistentId").getAsString();
    String name = jsonObject.get("name").getAsString();
    long creation = jsonObject.get("creation").getAsLong();
    long start = jsonObject.get("start").getAsLong();
    long end = jsonObject.get("end").getAsLong();
    EventState state = EventState.valueOf(jsonObject.get("state").getAsString());
    String location = jsonObject.get("location").getAsString();
    String description = jsonObject.get("description").getAsString();

    Set<PublicKey> attendees = new HashSet<>();
    JsonArray attendeesArray = jsonObject.getAsJsonArray("attendees");
    for (JsonElement attendee : attendeesArray) {
      PublicKey publicKey = new PublicKey(attendee.getAsString());
      attendees.add(publicKey);
    }

    return new RollCallBuilder()
        .setId(id)
        .setPersistentId(persistentId)
        .setName(name)
        .setCreation(creation)
        .setStart(start)
        .setEnd(end)
        .setState(state)
        .setAttendees(attendees)
        .setLocation(location)
        .setDescription(description)
        .build();
  }

  @Override
  public JsonElement serialize(RollCall src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject jsonObject = new JsonObject();

    jsonObject.addProperty("id", src.getId());
    jsonObject.addProperty("persistentId", src.getPersistentId());
    jsonObject.addProperty("name", src.getName());
    jsonObject.addProperty("creation", src.getCreation());
    jsonObject.addProperty("start", src.getStart());
    jsonObject.addProperty("end", src.getEnd());
    jsonObject.addProperty("state", src.getState().toString());
    jsonObject.addProperty("location", src.getLocation());
    jsonObject.addProperty("description", src.getDescription());

    JsonArray attendeesArray = new JsonArray();
    for (PublicKey attendee : src.getAttendees()) {
      String publicKeyString = attendee.getEncoded();
      attendeesArray.add(publicKeyString);
    }
    jsonObject.add("attendees", attendeesArray);

    return jsonObject;
  }
}
