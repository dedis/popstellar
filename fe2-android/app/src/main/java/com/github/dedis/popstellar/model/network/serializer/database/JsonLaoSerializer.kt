package com.github.dedis.popstellar.model.network.serializer.database

import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.ElectInstance
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.LaoBuilder
import com.github.dedis.popstellar.model.objects.PendingUpdate
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

/** This class serializes the LAO in a JSON string to be stored in the database  */
class JsonLaoSerializer : JsonSerializer<Lao>, JsonDeserializer<Lao> {
    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Lao {
        val jsonObject = json.asJsonObject

        // Deserialize the nested Channel object
        val channel = context.deserialize<Channel>(jsonObject["channel"], Channel::class.java)

        // Deserialize the other properties of the Lao object
        val id = jsonObject["id"].asString
        val name = jsonObject["name"].asString
        // LastModified could be absent
        val lastModifiedElement = jsonObject["lastModified"]
        val lastModified = lastModifiedElement?.asLong
        val creation = jsonObject["creation"].asLong
        val organizer = context.deserialize<PublicKey>(jsonObject["organizer"], PublicKey::class.java)
        val modificationId = context.deserialize<MessageID>(jsonObject["modificationId"], MessageID::class.java)

        // Deserialize the Set of PendingUpdate
        val pendingUpdatesJsonArray = jsonObject["pendingUpdates"].asJsonArray
        val pendingUpdates: MutableSet<PendingUpdate> = HashSet()
        for (pendingUpdatesJsonElement in pendingUpdatesJsonArray) {
            pendingUpdates.add(context.deserialize(pendingUpdatesJsonElement, PendingUpdate::class.java))
        }

        // Deserialize the Map of messageIdToElectInstance
        val messageIdToElectInstanceJsonObject = jsonObject["messageIdToElectInstance"].asJsonObject
        val messageIdToElectInstance: MutableMap<MessageID, ElectInstance> = HashMap()
        for ((key, value) in messageIdToElectInstanceJsonObject.entrySet()) {
            messageIdToElectInstance[MessageID(key)] = context.deserialize(value, ElectInstance::class.java)
        }

        /*
            TODO: The keyToNode is not serialized as the public keys are in base 28,
             they throw an error when encoded and decoded, so far it's not a problem as
             consensus is not used but it has to be fixed in the future
         */
        return LaoBuilder()
                .setChannel(channel)
                .setId(id)
                .setName(name)
                .setLastModified(lastModified)
                .setCreation(creation)
                .setOrganizer(organizer)
                .setModificationId(modificationId)
                .setPendingUpdates(pendingUpdates)
                .setMessageIdToElectInstance(messageIdToElectInstance) // .setKeyToNode(keyToNode)
                .build()
    }

    override fun serialize(lao: Lao, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()

        // Serialize the nested Channel object
        jsonObject.add("channel", context.serialize(lao.channel, Channel::class.java))

        // Serialize the other properties of the Lao object
        jsonObject.addProperty("id", lao.id)
        jsonObject.addProperty("name", lao.name)
        jsonObject.addProperty("lastModified", lao.lastModified)
        jsonObject.addProperty("creation", lao.creation)
        jsonObject.add("organizer", context.serialize(lao.organizer, PublicKey::class.java))
        jsonObject.add("modificationId", context.serialize(lao.modificationId, MessageID::class.java))

        // Serialize the Set of PendingUpdate
        val pendingUpdateJsonArray = JsonArray()
        for (pendingUpdate in lao.pendingUpdates) {
            pendingUpdateJsonArray.add(context.serialize(pendingUpdate, PendingUpdate::class.java))
        }
        jsonObject.add("pendingUpdates", pendingUpdateJsonArray)

        // Serialize the Map of messageIdToElectInstance
        val messageIdToElectInstanceJsonObject = JsonObject()
        for ((key, value) in lao.messageIdToElectInstance) {
            messageIdToElectInstanceJsonObject.add(
                    key.encoded, context.serialize(value, ElectInstance::class.java))
        }
        jsonObject.add("messageIdToElectInstance", messageIdToElectInstanceJsonObject)

        // TODO: add the keyToNode serialization when the deserialization will work
        return jsonObject
    }
}