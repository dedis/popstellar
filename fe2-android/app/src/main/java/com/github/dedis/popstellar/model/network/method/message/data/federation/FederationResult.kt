package com.github.dedis.popstellar.model.network.method.message.data.federation

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.google.gson.annotations.SerializedName

/** Informs about the result of the authentication procedure */
class FederationResult
/**
 * Constructor for a data Federation Result
 *
 * @param status status of the result (either success or failure)
 * @param reason reason of the failure
 * @param publicKey public key of the other LAO organizer
 * @param challenge challenge used to connect the LAOs
 */
(
    val status: String,
    val reason: String? = null,
    @SerializedName("public_key") val publicKey: String? = null,
    val challenge: MessageGeneral,
) : Data {

  init {
    when (status) {
      FAILURE -> {
        require(reason != null) { "Reason must be provided for $FAILURE status." }
        require(publicKey == null) { "Public key must be null for $FAILURE status." }
      }
      SUCCESS -> {
        require(publicKey != null) { "Public key must be provided for $SUCCESS status." }
        require(reason == null) { "Reason must be null for $SUCCESS status." }
      }
      else -> throw IllegalArgumentException("Status must be either '$FAILURE' or '$SUCCESS'.")
    }
  }

  override val `object`: String
    get() = Objects.FEDERATION.`object`

  override val action: String
    get() = Action.RESULT.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as FederationResult
    return status == that.status &&
        reason == that.reason &&
        publicKey == that.publicKey &&
        challenge == that.challenge
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(status, reason, publicKey, challenge)
  }

  override fun toString(): String {
    if (status == FAILURE) {
      return "FederationResult{status='$status', reason='$reason', challenge='$challenge'}"
    } else if (status == SUCCESS) {
      return "FederationResult{status='$status', public_key='$publicKey', " +
          "challenge='$challenge'}"
    }
    return "FederationResult{ERROR}"
  }

  fun isSuccess(): Boolean {
    return status == SUCCESS
  }

  companion object {
    private const val SUCCESS = "success"
    private const val FAILURE = "failure"
  }
}
