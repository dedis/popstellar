package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import androidx.annotation.Nullable;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.google.gson.annotations.SerializedName;
import com.github.dedis.popstellar.model.objects.Transaction;

import java.util.Optional;

/** Data sent to add a Chirp to the user channel */
public class AddTransaction extends Data {

  private final Transaction transaction;

  /**
   * Constructor for a data Post Transaction
   *
   * @param transaction is the transaction which is posted
   */
  public AddTransaction(Transaction transaction) {
    this.transaction = transaction;
  }

  @Override
  public String getObject() {
    return Objects.TRANSACTION.getObject();
  }

  @Override
  public String getAction() {
    return Action.ADD.getAction();
  }

  public Transaction getTransaction() {
    return transaction;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AddTransaction that = (AddTransaction) o;
    return java.util.Objects.equals(getTransaction(), that.getTransaction());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(getTransaction());
  }

  @Override
  public String toString() {
    return "AddTransaction{"
        + "transaction="
        + '\''
        + '}';
  }
}
