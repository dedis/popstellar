package com.github.dedis.popstellar.model.network.serializer;

import com.github.dedis.popstellar.model.objects.Channel;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class JsonChannelSerializer implements JsonSerializer<Channel>, JsonDeserializer<Channel> {

  @Override
  public Channel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    return Channel.newChannel(json.getAsString());
  }

  @Override
  public JsonElement serialize(Channel channel, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(channel.getAsString());
  }
}
