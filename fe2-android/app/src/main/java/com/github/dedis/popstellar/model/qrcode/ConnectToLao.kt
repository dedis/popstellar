package com.github.dedis.popstellar.model.qrcode

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.serializer.JsonUtils
import com.google.gson.Gson

/** Represent the data held in a QRCode used to connect to an LAO  */
@Immutable
class ConnectToLao(@JvmField val server: String, @JvmField val lao: String) {
    companion object {
        /**
         * Extract ConnectToLao data from the given json string
         *
         * @param gson is used to parse the json string into the object
         * @param json representation of the ConnectToLao data
         * @return the extracted ConnectToLao data
         * @throws com.google.gson.JsonParseException if the value cannot be parsed
         */
        @JvmStatic
        fun extractFrom(gson: Gson, json: String?): ConnectToLao {
            JsonUtils.verifyJson(JsonUtils.CONNECT_TO_LAO_SCHEMA, json)
            return gson.fromJson(json, ConnectToLao::class.java)
        }
    }
}