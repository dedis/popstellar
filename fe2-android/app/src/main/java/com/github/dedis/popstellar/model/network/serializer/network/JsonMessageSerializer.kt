package com.github.dedis.popstellar.model.network.serializer.network

import com.github.dedis.popstellar.model.network.method.Message
import com.github.dedis.popstellar.model.network.method.Method
import com.github.dedis.popstellar.model.network.method.Query
import com.github.dedis.popstellar.model.network.serializer.JsonUtils
import com.github.dedis.popstellar.model.network.serializer.JsonUtils.testRPCVersion
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import timber.log.Timber
import java.lang.reflect.Type

/** Json serializer and deserializer for the low level messages  */
class JsonMessageSerializer : JsonSerializer<Message>, JsonDeserializer<Message> {
    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Message {
        Timber.tag(TAG).d("deserializing message")
        val container = context.deserialize<JSONRPCRequest>(json, JSONRPCRequest::class.java)
        testRPCVersion(container.jsonrpc)
        val method = Method.find(container.method)
                ?: throw JsonParseException("Unknown method type " + container.method)
        val params = container.params

        // If the Channeled Data is a Query, we need to give the params the id the the request
        if (method.expectResult()) {
            params.add(JsonUtils.JSON_REQUEST_ID, json.asJsonObject[JsonUtils.JSON_REQUEST_ID])
        }
        return context.deserialize(params, method.dataClass)
    }

    override fun serialize(src: Message, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val params = context.serialize(src).asJsonObject
        val obj = context
                .serialize(JSONRPCRequest(JsonUtils.JSON_RPC_VERSION, src.method, params))
                .asJsonObject
        if (src is Query) {
            obj.addProperty(JsonUtils.JSON_REQUEST_ID, src.requestId)
        }
        return obj
    }

    companion object {
        private val TAG = JsonMessageSerializer::class.java.simpleName
    }
}