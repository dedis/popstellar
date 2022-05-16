package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.google.gson.annotations.SerializedName;

public class PostTransactionCoin extends Data {
  @SerializedName(value = "transaction_id")
  private final String transaction_id; // TxOutHash SHA256 over base64encode(transaction)

  @SerializedName(value = "transaction")
  private final Transaction transaction; // the transaction object // String

  /**
   * @param transaction the transaction object
   */
  public PostTransactionCoin(Transaction transaction) {
    this.transaction = transaction;
    this.transaction_id = transaction.computeId();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PostTransactionCoin that = (PostTransactionCoin) o;
    return java.util.Objects.equals(transaction, that.transaction);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(get_transaction_id(), get_transaction());
  }

  @Override
  public String toString() {
    return "PostTransactionCoin{ transaction_id="
        + transaction_id
        + "transaction="
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

  public Transaction get_transaction() {
    return transaction;
  }

  public String get_transaction_id() {
    return transaction_id;
  }
}
