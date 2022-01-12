package com.github.dedis.popstellar.model.network.serializer;

import com.github.dedis.popstellar.model.objects.security.Base64URLData;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.function.Function;

public class JsonBase64DataSerializer<T extends Base64URLData>
    implements JsonSerializer<T>, JsonDeserializer<T> {

  private final Function<String, T> constructor;

  public JsonBase64DataSerializer(Function<String, T> constructor) {
    this.constructor = constructor;
  }

  @Override
  public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    return constructor.apply(json.getAsString());
  }

  @Override
  public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.getEncoded());
  }
}
