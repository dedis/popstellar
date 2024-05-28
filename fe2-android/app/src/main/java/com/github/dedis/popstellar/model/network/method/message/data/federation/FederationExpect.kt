package com.github.dedis.popstellar.model.network.method.message.data.federation

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.google.gson.annotations.SerializedName

/** Federation Expect message */
class FederationExpect
/**
 * Constructor for a data Federation Expect
 *
 * @param laoId ID of the remote LAO
 * @param serverAddress public address of the remote organizer server
 * @param publicKey public key of the remote organizer
 * @param challenge challenge for the server
 */
(
    @SerializedName("lao_id") val laoId: String,
    @SerializedName("server_address") val serverAddress: String,
    @SerializedName("public_key") val publicKey: String,
    val challenge: MessageGeneral
) : Data {

  override val `object`: String
    get() = Objects.FEDERATION.`object`

  override val action: String
    get() = Action.EXPECT.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as FederationExpect
    return laoId == that.laoId &&
        serverAddress == that.serverAddress &&
        publicKey == that.publicKey &&
        challenge == that.challenge
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(laoId, serverAddress, publicKey, challenge)
  }

  override fun toString(): String {
    return "FederationExpect{lao_id='$laoId', server_address='$serverAddress'," +
        "public_key='$publicKey', challenge='$challenge'}"
  }
}
