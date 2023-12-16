package com.github.dedis.popstellar.model.network.serializer.base64

import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.util.function.Function

class JsonBase64DataSerializer<T : Base64URLData?>(private val constructor: Function<String, T>) : JsonSerializer<T>, JsonDeserializer<T> {
    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): T {
        return try {
            constructor.apply(json.asString)
        } catch (e: Exception) {
            throw JsonParseException(e)
        }
    }

    override fun serialize(src: T, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src!!.encoded)
    }
}