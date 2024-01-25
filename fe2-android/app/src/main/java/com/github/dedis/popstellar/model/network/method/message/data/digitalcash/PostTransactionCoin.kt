package com.github.dedis.popstellar.model.network.method.message.data.digitalcash

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.google.gson.annotations.SerializedName
import java.util.Objects

@Immutable
class PostTransactionCoin( // the transaction object // String
    @field:SerializedName(value = "transaction") val transaction: Transaction
) : Data {
  @SerializedName(value = "transaction_id")
  val transactionId // TxOutHash SHA256 over base64encode(transaction)
  : String =
      transaction.computeId()

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as PostTransactionCoin
    return transaction == that.transaction
  }

  override fun hashCode(): Int {
    return Objects.hash(transactionId, transaction)
  }

  override fun toString(): String {
    return "PostTransactionCoin{ transaction_id=$transactionId, transaction=$transaction}"
  }

  override val `object`: String =
      com.github.dedis.popstellar.model.network.method.message.data.Objects.COIN.`object`
  override val action: String = Action.POST_TRANSACTION.action
}
