package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.google.gson.annotations.SerializedName;

public class PostTransaction extends Data {
  @SerializedName(value = "transaction")
  private final Transaction transaction; // the transaction object

  /**
   * @param transaction the transaction object
   */
  public PostTransaction(Transaction transaction) {
    this.transaction = transaction;
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
