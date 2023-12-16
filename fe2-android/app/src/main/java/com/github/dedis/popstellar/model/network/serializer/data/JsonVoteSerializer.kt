package com.github.dedis.popstellar.model.network.serializer.data

import com.github.dedis.popstellar.model.network.method.message.data.election.EncryptedVote
import com.github.dedis.popstellar.model.network.method.message.data.election.PlainVote
import com.github.dedis.popstellar.model.network.method.message.data.election.Vote
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

/** Simple serializer to convert json elements to Vote types and vice-versa  */
class JsonVoteSerializer : JsonSerializer<Vote?>, JsonDeserializer<Vote> {
    override fun serialize(src: Vote?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        // make sure the actual class of the vote is used for serialization.
        // Even if the asked type is Vote
        return context.serialize(src)
    }

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Vote {
        val inter = context.deserialize<IntermediateVote>(json, IntermediateVote::class.java)
        return inter.convert()
    }

    @Suppress("unused")
    private class IntermediateVote {
        private val id: String? = null
        private val question: String? = null
        private val vote: JsonPrimitive? = null
        fun convert(): Vote {
            return if (vote!!.isString) {
                EncryptedVote(id, question, vote.asString)
            } else if (vote.isNumber) {
                PlainVote(id, question, vote.asInt)
            } else {
                throw JsonParseException("Unknown vote type")
            }
        }
    }
}