package com.github.dedis.popstellar.model.qrcode;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.serializer.JsonUtils;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

@Immutable
public class MainPublicKeyData {

    @SerializedName("main_public_key")
    private final PublicKey publicKey;

    public MainPublicKeyData(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public PublicKey getPublicKey(){
        return publicKey;
    }

    /**
     * Extract data from the given json string
     *
     * @param gson is used to parse the json string into the object
     * @param json representation of the data
     * @return the extracted data
     * @throws com.google.gson.JsonParseException if the value cannot be parsed
     */
    public static MainPublicKeyData extractFrom(Gson gson, String json) {
        JsonUtils.verifyJson(JsonUtils.MAIN_PK_SCHEME, json);
        return gson.fromJson(json, MainPublicKeyData.class);
    }
}
