package com.github.dedis.popstellar.model.network.serializer;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Answer;
import com.github.dedis.popstellar.model.network.method.Message;
import com.google.gson.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Type;

/** Json deserializer for the generic messages */
public class JsonGenericMessageDeserializer implements JsonDeserializer<GenericMessage> {
  private static final Logger logger = LogManager.getLogger(JsonGenericMessageDeserializer.class);
  private static final String METHOD = "method";

  @Override
  public GenericMessage deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    logger.debug("deserializing generic message");
    if (json.getAsJsonObject().has(METHOD)) {
      return context.deserialize(json, Message.class);
    } else {
      return context.deserialize(json, Answer.class);
    }
  }
}
