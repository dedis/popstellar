package com.github.dedis.popstellar.model.network.serializer.network

import com.github.dedis.popstellar.model.network.answer.Answer
import com.github.dedis.popstellar.model.network.answer.Error
import com.github.dedis.popstellar.model.network.answer.Result
import com.github.dedis.popstellar.model.network.serializer.JsonUtils
import com.github.dedis.popstellar.model.network.serializer.JsonUtils.testRPCVersion
import com.github.dedis.popstellar.model.network.serializer.JsonUtils.verifyJson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

/** Json serializer and deserializer for the answer */
class JsonAnswerSerializer : JsonSerializer<Answer?>, JsonDeserializer<Answer> {
  @Throws(JsonParseException::class)
  override fun deserialize(
    json: JsonElement,
    typeOfT: Type,
    context: JsonDeserializationContext
  ): Answer {
    val obj = json.asJsonObject

    testRPCVersion(obj)
    verifyJson(JsonUtils.ROOT_SCHEMA, json.toString())

    return if (obj.has(RESULT)) {
      context.deserialize(json, Result::class.java)
    } else if (obj.has(ERROR)) {
      context.deserialize<Answer>(json, Error::class.java)
    } else {
      throw JsonParseException("A result must contain one of the field result or error")
    }
  }

  override fun serialize(
    src: Answer?,
    typeOfSrc: Type,
    context: JsonSerializationContext
  ): JsonElement {
    val obj = context.serialize(src).asJsonObject
    obj.addProperty(JsonUtils.JSON_RPC, JsonUtils.JSON_RPC_VERSION)

    verifyJson(JsonUtils.ROOT_SCHEMA, obj.toString())

    return obj
  }

  companion object {
    private const val RESULT = "result"
    private const val ERROR = "error"
  }
}
