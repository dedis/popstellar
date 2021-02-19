package com.github.dedis.student20_pop.utility.json;

import android.util.Base64;

import com.github.dedis.student20_pop.model.network.method.Message;
import com.github.dedis.student20_pop.model.network.method.message.MessageGeneral;
import com.github.dedis.student20_pop.model.network.method.message.PublicKeySignaturePair;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.List;

public class JsonMessageGeneralSerializer implements JsonSerializer<MessageGeneral>, JsonDeserializer<MessageGeneral> {
    @Override
    public MessageGeneral deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject root = json.getAsJsonObject();

        byte[] messageId = Base64.decode(root.get("message_id").getAsString(), Base64.NO_WRAP);
        byte[] dataBuf = Base64.decode(root.get("data").getAsString(), Base64.NO_WRAP);
        byte[] sender = Base64.decode(root.get("sender").getAsString(), Base64.NO_WRAP);
        byte[] signature = Base64.decode(root.get("signature").getAsString(), Base64.NO_WRAP);

        List<PublicKeySignaturePair> witnessSignatures = context.deserialize(root.get("witness_signatures"), PublicKeySignaturePair.class);

        JsonElement dataElement = JsonParser.parseString(new String(dataBuf));
        Data data = context.deserialize(dataElement, Data.class);

        return new MessageGeneral(
                sender,
                dataBuf,
                data,
                signature,
                messageId,
                witnessSignatures
        );
    }

    @Override
    public JsonElement serialize(MessageGeneral src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();

        result.addProperty("message_id", src.getMessageId());
        result.addProperty("sender", src.getSender());
        result.addProperty("signature", src.getSignature());


        result.addProperty("data", src.getDataEncoded());
        result.add("witness_signatures", context.serialize(src.getWitnessSignatures()));

        return result;
    }
}
