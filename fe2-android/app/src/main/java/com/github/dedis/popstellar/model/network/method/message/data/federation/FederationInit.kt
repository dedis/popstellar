package com.github.dedis.popstellar.model.network.method.message.data.federation

import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.google.gson.annotations.SerializedName

/** Initiates a federation link */
class FederationInit : Data {
  @SerializedName("lao_id") val laoId: String
  @SerializedName("server_address") val serverAddress: String
  @SerializedName("public_key") val publicKey: String
  val challenge: Challenge

  /**
   * Constructor for a data Federation Init
   *
   * @param laoId ID of the remote LAO
   * @param serverAddress public address of the remote organizer server
   * @param publicKey public key of the remote organizer
   * @param challenge challenge from the other server
   */
  constructor(laoId: String, serverAddress: String, publicKey: String, challenge: Challenge) {
    this.laoId = laoId
    this.serverAddress = serverAddress
    this.publicKey = publicKey
    this.challenge = challenge
  }

  override val `object`: String
    get() = Objects.FEDERATION.`object`

  override val action: String
    get() = Action.INIT.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as FederationInit
    return laoId == that.laoId &&
        serverAddress == that.serverAddress &&
        publicKey == that.publicKey &&
        challenge == that.challenge
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(laoId, serverAddress, publicKey, challenge)
  }

  override fun toString(): String {
    return "FederationInit{lao_id='$laoId', server_address='$serverAddress'," +
        "public_key='$publicKey', challenge='$challenge'}"
  }
}
