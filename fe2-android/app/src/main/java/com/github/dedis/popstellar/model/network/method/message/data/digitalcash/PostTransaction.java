package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.google.gson.annotations.SerializedName;

import java.util.Base64;

public class PostTransaction extends Data {
  @SerializedName(value = "transaction_id")
  private final String transaction_id; // TxOutHash SHA256 over base64encode(transaction)

  @SerializedName(value = "transaction")
  private final Transaction transaction; // the transaction object // String

  /**
   * @param transaction the transaction object
   */
  public PostTransaction(Transaction transaction) {
    this.transaction = transaction;
    this.transaction_id = Base64.getEncoder().encodeToString(transaction.toString().getBytes());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PostTransaction that = (PostTransaction) o;
    return java.util.Objects.equals(transaction, that.transaction);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(getTransaction());
  }

  @Override
  public String toString() {
    return "PostTransaction{" + "transaction=" + transaction + '\'' + '}';
  }

  @Override
  public String getObject() {
    return Objects.TRANSACTION.getObject();
  }

  @Override
  public String getAction() {
    return Action.POST.getAction();
  }

  public Transaction getTransaction() {
    return transaction;
  }
}
