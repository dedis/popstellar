package com.github.dedis.popstellar.model.qrcode;

import com.github.dedis.popstellar.model.network.serializer.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class MainPublicKeyData {

    @SerializedName("identity")
    private final String publicKey;

    public MainPublicKeyData(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicKey(){
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
