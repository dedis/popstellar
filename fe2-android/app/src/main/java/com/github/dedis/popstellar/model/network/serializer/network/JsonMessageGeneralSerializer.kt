package com.github.dedis.popstellar.model.network.serializer.network

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.PublicKeySignaturePair
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.serializer.JsonUtils
import com.github.dedis.popstellar.model.network.serializer.JsonUtils.verifyJson
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.security.Signature
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets

class JsonMessageGeneralSerializer :
  JsonSerializer<MessageGeneral>, JsonDeserializer<MessageGeneral> {
  @Throws(JsonParseException::class)
  override fun deserialize(
    json: JsonElement,
    typeOfT: Type,
    context: JsonDeserializationContext
  ): MessageGeneral {
    val jsonObject = context.deserialize<JsonMessageData>(json, JsonMessageData::class.java)
    val dataElement = JsonParser.parseString(jsonObject.data.data.toString(StandardCharsets.UTF_8))
    val data = context.deserialize<Data>(dataElement, Data::class.java)

    return MessageGeneral(
      jsonObject.sender,
      jsonObject.data,
      data,
      jsonObject.signature,
      jsonObject.messageID,
      jsonObject.witnessSignatures
    )
  }

  override fun serialize(
    src: MessageGeneral,
    typeOfSrc: Type,
    context: JsonSerializationContext
  ): JsonElement {
    val jsonObject =
      JsonMessageData(
        src.dataEncoded,
        src.sender,
        src.signature,
        src.messageId,
        src.witnessSignatures
      )
    val result = context.serialize(jsonObject)

    verifyJson(JsonUtils.GENERAL_MESSAGE_SCHEMA, result.toString())

    return result
  }

  private class JsonMessageData(
    val data: Base64URLData,
    val sender: PublicKey,
    val signature: Signature,
    @field:SerializedName("message_id") val messageID: MessageID,
    @field:SerializedName("witness_signatures") val witnessSignatures: List<PublicKeySignaturePair>
  )
}
