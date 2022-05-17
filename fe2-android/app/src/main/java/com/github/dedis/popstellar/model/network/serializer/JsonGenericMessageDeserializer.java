package com.github.dedis.popstellar.model.network.serializer;

import android.util.Log;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Answer;
import com.github.dedis.popstellar.model.network.method.Message;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/** Json deserializer for the generic messages */
public class JsonGenericMessageDeserializer implements JsonDeserializer<GenericMessage> {
  public static final String TAG = JsonGenericMessageDeserializer.class.getSimpleName();
  private static final String METHOD = "method";

  @Override
  public GenericMessage deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    Log.d(TAG, "deserializing generic message");
    if (json.getAsJsonObject().has(METHOD)) {
      return context.deserialize(json, Message.class);
    } else {
      return context.deserialize(json, Answer.class);
    }
  }
}
