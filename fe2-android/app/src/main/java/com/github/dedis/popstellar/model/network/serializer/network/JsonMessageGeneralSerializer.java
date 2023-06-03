package com.github.dedis.popstellar.model.network.serializer.network;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.PublicKeySignaturePair;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.serializer.JsonUtils;
import com.github.dedis.popstellar.model.objects.security.*;
import com.google.gson.*;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class JsonMessageGeneralSerializer
    implements JsonSerializer<MessageGeneral>, JsonDeserializer<MessageGeneral> {

  @Override
  public MessageGeneral deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonMessageData jsonObject = context.deserialize(json, JsonMessageData.class);

    JsonElement dataElement =
        JsonParser.parseString(new String(jsonObject.data.getData(), StandardCharsets.UTF_8));
    Data data = context.deserialize(dataElement, Data.class);

    return new MessageGeneral(
        jsonObject.sender,
        jsonObject.data,
        data,
        jsonObject.signature,
        jsonObject.messageID,
        jsonObject.witnessSignatures);
  }

  @Override
  public JsonElement serialize(
      MessageGeneral src, Type typeOfSrc, JsonSerializationContext context) {
    JsonMessageData jsonObject =
        new JsonMessageData(
            src.getDataEncoded(),
            src.getSender(),
            src.getSignature(),
            src.getMessageId(),
            src.getWitnessSignatures());
    JsonElement result = context.serialize(jsonObject);
    JsonUtils.verifyJson(JsonUtils.GENERAL_MESSAGE_SCHEMA, result.toString());
    return result;
  }

  private static final class JsonMessageData {

    public final Base64URLData data;
    public final PublicKey sender;
    public final Signature signature;

    @SerializedName("message_id")
    public final MessageID messageID;

    @SerializedName("witness_signatures")
    public final List<PublicKeySignaturePair> witnessSignatures;

    private JsonMessageData(
        Base64URLData data,
        PublicKey sender,
        Signature signature,
        MessageID messageID,
        List<PublicKeySignaturePair> witnessSignatures) {
      this.data = data;
      this.sender = sender;
      this.signature = signature;
      this.messageID = messageID;
      this.witnessSignatures = witnessSignatures;
    }
  }
}
