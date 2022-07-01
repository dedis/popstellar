package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.google.gson.annotations.SerializedName;

public final class PostTransactionCoin extends Data {

  @SerializedName(value = "transaction_id")
  private final String transactionId; // TxOutHash SHA256 over base64encode(transaction)

  @SerializedName(value = "transaction")
  private final Transaction transaction; // the transaction object // String

  /**
   * @param transaction the transaction object
   */
  public PostTransactionCoin(Transaction transaction) {
    this.transaction = transaction;
    this.transactionId = transaction.computeId();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PostTransactionCoin that = (PostTransactionCoin) o;
    return java.util.Objects.equals(transaction, that.transaction);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(getTransactionId(), getTransaction());
  }

  @Override
  public String toString() {
    return "PostTransactionCoin{ transaction_id="
        + transactionId
        + ", transaction="
        + transaction
        + '}';
  }

  @Override
  public String getObject() {
    return Objects.COIN.getObject();
  }

  @Override
  public String getAction() {
    return Action.POST_TRANSACTION.getAction();
  }

  public Transaction getTransaction() {
    return transaction;
  }

  public String getTransactionId() {
    return transactionId;
  }
}
