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

  private static final String METHOD = "method";

  @Override
  public GenericMessage deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    Log.d("deserializer", "deserializing generic message : " + json.toString());

    GenericMessage msg =
        json.getAsJsonObject().has(METHOD)
            ? context.deserialize(json, Message.class)
            : context.deserialize(json, Answer.class);

    Log.d("deserializer", "deserialized generic message : " + msg);

    return msg;
  }
}
