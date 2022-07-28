package com.github.dedis.popstellar.model.network.serializer;

import com.github.dedis.popstellar.model.objects.security.Base64URLData;
import com.google.gson.*;

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
    try {
      return constructor.apply(json.getAsString());
    } catch (Exception e) {
      throw new JsonParseException(e);
    }
  }

  @Override
  public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.getEncoded());
  }
}
