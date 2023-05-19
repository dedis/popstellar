package com.github.dedis.popstellar.model.network.serializer.database;

import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class JsonTransactionObjectSerializer
    implements JsonSerializer<TransactionObject>, JsonDeserializer<TransactionObject> {

  @Override
  public TransactionObject deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();

    // Deserialize the nested Channel object
    Channel channel = context.deserialize(jsonObject.get("channel"), Channel.class);

    // Deserialize the primitive values
    int version = jsonObject.get("version").getAsInt();
    long lockTime = jsonObject.get("lockTime").getAsLong();
    String transactionId = jsonObject.get("transactionId").getAsString();

    // Deserialize the inputs
    JsonArray inputsJsonArray = jsonObject.get("inputs").getAsJsonArray();
    List<InputObject> inputs = new ArrayList<>();
    for (JsonElement inputElement : inputsJsonArray) {
      inputs.add(context.deserialize(inputElement, InputObject.class));
    }

    // Deserialize the outputs
    JsonArray outputsJsonArray = jsonObject.get("outputs").getAsJsonArray();
    List<OutputObject> outputs = new ArrayList<>();
    for (JsonElement outputElement : outputsJsonArray) {
      outputs.add(context.deserialize(outputElement, OutputObject.class));
    }

    return new TransactionObject(channel, version, inputs, outputs, lockTime, transactionId);
  }

  @Override
  public JsonElement serialize(
      TransactionObject transactionObject, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject jsonObject = new JsonObject();

    // Serialize the channel object
    jsonObject.add("channel", context.serialize(transactionObject.getChannel(), Channel.class));

    // Serialize the primitive values
    jsonObject.addProperty("version", transactionObject.getVersion());
    jsonObject.addProperty("lockTime", transactionObject.getLockTime());
    jsonObject.addProperty("transactionId", transactionObject.getTransactionId());

    // Serialize the inputs
    JsonArray inputsJsonArray = new JsonArray();
    for (InputObject inputObject : transactionObject.getInputs()) {
      inputsJsonArray.add(context.serialize(inputObject, InputObject.class));
    }
    jsonObject.add("inputs", inputsJsonArray);

    // Serialize the outputs
    JsonArray outputsJsonArray = new JsonArray();
    for (OutputObject outputObject : transactionObject.getOutputs()) {
      outputsJsonArray.add(context.serialize(outputObject, OutputObject.class));
    }
    jsonObject.add("outputs", outputsJsonArray);

    return jsonObject;
  }
}
