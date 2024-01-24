package com.github.dedis.popstellar.model.network.serializer.network

import com.github.dedis.popstellar.model.network.GenericMessage
import com.github.dedis.popstellar.model.network.answer.Answer
import com.github.dedis.popstellar.model.network.method.Message
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type
import timber.log.Timber

/** Json deserializer for the generic messages */
class JsonGenericMessageDeserializer : JsonDeserializer<GenericMessage> {
  @Throws(JsonParseException::class)
  override fun deserialize(
      json: JsonElement,
      typeOfT: Type,
      context: JsonDeserializationContext
  ): GenericMessage {
    Timber.tag(TAG).d("deserializing generic message")

    return if (json.asJsonObject.has(METHOD)) {
      context.deserialize(json, Message::class.java)
    } else {
      context.deserialize(json, Answer::class.java)
    }
  }

  companion object {
    val TAG: String = JsonGenericMessageDeserializer::class.java.simpleName
    private const val METHOD = "method"
  }
}
