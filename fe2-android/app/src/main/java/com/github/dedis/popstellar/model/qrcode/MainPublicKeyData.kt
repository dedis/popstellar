package com.github.dedis.popstellar.model.qrcode

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.serializer.JsonUtils
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

@Immutable
class MainPublicKeyData(@JvmField @field:SerializedName("main_public_key") val publicKey: PublicKey) {

    companion object {
        /**
         * Extract data from the given json string
         *
         * @param gson is used to parse the json string into the object
         * @param json representation of the data
         * @return the extracted data
         * @throws com.google.gson.JsonParseException if the value cannot be parsed
         */
        @JvmStatic
        fun extractFrom(gson: Gson, json: String?): MainPublicKeyData {
            JsonUtils.verifyJson(JsonUtils.MAIN_PK_SCHEME, json)
            return gson.fromJson(json, MainPublicKeyData::class.java)
        }
    }
}