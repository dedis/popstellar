package com.github.dedis.popstellar.model.network.method.message.data.federation

import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.google.gson.annotations.SerializedName

/** Token exchange to be broadcast in the LAO */
class TokensExchange
/**
 * Constructor for a data TokenExchange
 *
 * @param laoId ID of the remote LAO
 * @param rollCallId ID of the rollCall of the remote LAO
 * @param tokens array of tokens contained in the rollCall
 * @param timestamp timestamp of the message
 */
(
    @SerializedName("lao_id") val laoId: String,
    @SerializedName("roll_call_id") val rollCallId: String,
    val tokens: Array<String>,
    val timestamp: Long
) : Data {
  override val `object`: String
    get() = Objects.FEDERATION.`object`

  override val action: String
    get() = Action.TOKENS_EXCHANGE.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as TokensExchange
    return laoId == that.laoId &&
        rollCallId == that.rollCallId &&
        tokens.contentEquals(that.tokens) &&
        timestamp == that.timestamp
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(laoId, rollCallId, tokens, timestamp)
  }

  override fun toString(): String {
    return "TokensExchange{lao_id='$laoId', roll_call_id='$rollCallId'," +
        "tokens='$tokens', timestamp='$timestamp'}"
  }
}
