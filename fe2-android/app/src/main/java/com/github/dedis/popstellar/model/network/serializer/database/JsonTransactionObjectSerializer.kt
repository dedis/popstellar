package com.github.dedis.popstellar.model.network.serializer.database

import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.InputObject
import com.github.dedis.popstellar.model.objects.OutputObject
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

class JsonTransactionObjectSerializer :
  JsonSerializer<TransactionObject>, JsonDeserializer<TransactionObject> {
  @Throws(JsonParseException::class)
  override fun deserialize(
    json: JsonElement,
    typeOfT: Type,
    context: JsonDeserializationContext
  ): TransactionObject {
    val jsonObject = json.asJsonObject

    // Deserialize the nested Channel object
    val channel = context.deserialize<Channel>(jsonObject["channel"], Channel::class.java)

    // Deserialize the primitive values
    val version = jsonObject["version"].asInt
    val lockTime = jsonObject["lockTime"].asLong
    val transactionId = jsonObject["transactionId"].asString

    // Deserialize the inputs
    val inputsJsonArray = jsonObject["inputs"].asJsonArray
    val inputs: MutableList<InputObject> = ArrayList()
    for (inputElement in inputsJsonArray) {
      inputs.add(context.deserialize(inputElement, InputObject::class.java))
    }

    // Deserialize the outputs
    val outputsJsonArray = jsonObject["outputs"].asJsonArray
    val outputs: MutableList<OutputObject> = ArrayList()
    for (outputElement in outputsJsonArray) {
      outputs.add(context.deserialize(outputElement, OutputObject::class.java))
    }

    return TransactionObject(channel, version, inputs, outputs, lockTime, transactionId)
  }

  override fun serialize(
    transactionObject: TransactionObject,
    typeOfSrc: Type,
    context: JsonSerializationContext
  ): JsonElement {
    val jsonObject = JsonObject()

    // Serialize the channel object
    jsonObject.add("channel", context.serialize(transactionObject.channel, Channel::class.java))

    // Serialize the primitive values
    jsonObject.addProperty("version", transactionObject.version)
    jsonObject.addProperty("lockTime", transactionObject.lockTime)
    jsonObject.addProperty("transactionId", transactionObject.transactionId)

    // Serialize the inputs
    val inputsJsonArray = JsonArray()
    for (inputObject in transactionObject.inputs) {
      inputsJsonArray.add(context.serialize(inputObject, InputObject::class.java))
    }
    jsonObject.add("inputs", inputsJsonArray)

    // Serialize the outputs
    val outputsJsonArray = JsonArray()
    for (outputObject in transactionObject.outputs) {
      outputsJsonArray.add(context.serialize(outputObject, OutputObject::class.java))
    }
    jsonObject.add("outputs", outputsJsonArray)

    return jsonObject
  }
}
