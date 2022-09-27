package com.github.dedis.popstellar.model.qrcode;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.serializer.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * Represent the data held in a QRCode used to display roll call tokens
 */
@Immutable
public class PopTokenData {

    @SerializedName("poptoken")
    private final String popToken;

    public PopTokenData(String popToken){
        this.popToken = popToken;
    }

    /**
     * Extract data from the given json string
     *
     * @param gson is used to parse the json string into the object
     * @param json representation of the data
     * @return the extracted data
     * @throws com.google.gson.JsonParseException if the value cannot be parsed
     */
    public static PopTokenData extractFrom(Gson gson, String json) {
        JsonUtils.verifyJson(JsonUtils.POP_TOKEN_SCHEME, json);
        return gson.fromJson(json, PopTokenData.class);
    }

    public String getPopToken(){
        return popToken;
    }
}
