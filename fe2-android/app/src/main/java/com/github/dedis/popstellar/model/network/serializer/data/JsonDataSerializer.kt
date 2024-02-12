package com.github.dedis.popstellar.model.network.serializer.data

import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.network.serializer.JsonUtils
import com.github.dedis.popstellar.model.network.serializer.JsonUtils.verifyJson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

/** Json serializer and deserializer for the data messages */
class JsonDataSerializer(private val dataRegistry: DataRegistry) :
    JsonSerializer<Data>, JsonDeserializer<Data> {
  @Throws(JsonParseException::class)
  override fun deserialize(
      json: JsonElement,
      typeOfT: Type,
      context: JsonDeserializationContext
  ): Data {
    val obj = json.asJsonObject
    verifyJson(JsonUtils.DATA_SCHEMA, obj.toString())

    val `object` = Objects.find(obj[OBJECT].asString)
    val action = Action.find(obj[ACTION].asString)

    if (`object` == null) {
      throw JsonParseException("Unknown object type : " + obj[OBJECT].asString)
    }
    if (action == null) {
      throw JsonParseException("Unknown action type : " + obj[ACTION].asString)
    }

    val clazz = dataRegistry.getType(`object`, action)
    if (!clazz.isPresent) {
      throw JsonParseException(
          "The pair (${`object`.`object`}, ${action.action}) does not exists in the protocol")
    }

    return context.deserialize(json, clazz.get())
  }

  override fun serialize(
      src: Data,
      typeOfSrc: Type,
      context: JsonSerializationContext
  ): JsonElement {
    val obj = context.serialize(src).asJsonObject

    obj.addProperty(OBJECT, src.`object`)
    obj.addProperty(ACTION, src.action)

    verifyJson(JsonUtils.DATA_SCHEMA, obj.toString())

    return obj
  }

  companion object {
    private const val OBJECT = "object"
    private const val ACTION = "action"
  }
}
