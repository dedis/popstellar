package com.github.dedis.popstellar.model.qrcode

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.federation.Challenge
import com.github.dedis.popstellar.model.network.serializer.JsonUtils
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

@Immutable
class FederationDetails(
    @field:SerializedName("lao_id") val laoId: String,
    @field:SerializedName("server_address") val serverAddress: String,
    @field:SerializedName("public_key") val publicKey: String,
    val challenge: Challenge? = null
) {

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
    fun extractFrom(gson: Gson, json: String?): FederationDetails {
      JsonUtils.verifyJson(JsonUtils.FEDERATION_DETAILS, json)

      return gson.fromJson(json, FederationDetails::class.java)
    }
  }
}
