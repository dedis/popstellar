package com.github.dedis.popstellar.model.network.serializer.network

import com.github.dedis.popstellar.model.network.answer.Result
import com.github.dedis.popstellar.model.network.answer.ResultMessages
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class JsonResultSerializer : JsonSerializer<Result>, JsonDeserializer<Result> {
  @Throws(JsonParseException::class)
  override fun deserialize(
    json: JsonElement,
    typeOfT: Type,
    context: JsonDeserializationContext
  ): Result {
    val root = json.asJsonObject
    val id = root["id"].asInt
    val resultElement = root[RESULT]

    return if (resultElement.isJsonPrimitive) {
      Result(id)
    } else {
      val listType = object : TypeToken<ArrayList<MessageGeneral>?>() {}.type
      val messages = context.deserialize<List<MessageGeneral>>(resultElement.asJsonArray, listType)
      ResultMessages(id, messages)
    }
  }

  override fun serialize(
    src: Result,
    typeOfSrc: Type,
    context: JsonSerializationContext
  ): JsonElement {
    val output = JsonObject()
    output.addProperty(ID, src.id)

    if (src is ResultMessages) {
      val messages = context.serialize(src.messages)
      output.add(RESULT, messages)
    } else {
      output.addProperty(RESULT, 0)
    }

    return output
  }

  companion object {
    private const val RESULT = "result"
    private const val ID = "ID"
  }
}
