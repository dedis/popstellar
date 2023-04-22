package com.github.dedis.popstellar.model.network.serializer;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Answer;
import com.github.dedis.popstellar.model.network.method.Message;
import com.google.gson.*;

import java.lang.reflect.Type;

import timber.log.Timber;

/** Json deserializer for the generic messages */
public class JsonGenericMessageDeserializer implements JsonDeserializer<GenericMessage> {
  public static final String TAG = JsonGenericMessageDeserializer.class.getSimpleName();
  private static final String METHOD = "method";

  @Override
  public GenericMessage deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    Timber.tag(TAG).d("deserializing generic message");
    if (json.getAsJsonObject().has(METHOD)) {
      return context.deserialize(json, Message.class);
    } else {
      return context.deserialize(json, Answer.class);
    }
  }
}
