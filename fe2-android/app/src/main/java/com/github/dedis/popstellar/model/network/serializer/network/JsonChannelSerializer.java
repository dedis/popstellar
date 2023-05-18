package com.github.dedis.popstellar.model.network.serializer.network;

import com.github.dedis.popstellar.model.objects.Channel;
import com.google.gson.*;

import java.lang.reflect.Type;

public class JsonChannelSerializer implements JsonSerializer<Channel>, JsonDeserializer<Channel> {

  @Override
  public Channel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    return Channel.fromString(json.getAsString());
  }

  @Override
  public JsonElement serialize(Channel channel, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(channel.getAsString());
  }
}
