package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import android.view.SurfaceControl;

import androidx.annotation.Nullable;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.Address;
import com.google.gson.annotations.SerializedName;

public class AddTransaction extends Data {
    @SerializedName(value = "transaction")
    private final Transaction transaction; //the transaction object

  /**
   * @param transaction the transaction object
   */
  public AddTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddTransaction that = (AddTransaction) o;
        return java.util.Objects.equals(transaction, that.transaction);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getTransaction());
    }

    @Override
    public String toString() {
        return "AddTransaction{" +
                "transaction=" + transaction + '\'' +
                '}';
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
