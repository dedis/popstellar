package com.github.dedis.popstellar.model.network.serializer.network

import com.github.dedis.popstellar.model.objects.Channel
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

class JsonChannelSerializer : JsonSerializer<Channel>, JsonDeserializer<Channel> {
    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Channel {
        return Channel.fromString(json.asString)
    }

    override fun serialize(channel: Channel, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(channel.asString)
    }
}