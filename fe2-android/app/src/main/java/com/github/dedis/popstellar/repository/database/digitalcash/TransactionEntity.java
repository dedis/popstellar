package com.github.dedis.popstellar.repository.database.digitalcash;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject;

@Entity(tableName = "transactions")
public class TransactionEntity {

  @PrimaryKey
  @ColumnInfo(name = "transaction_id")
  @NonNull
  private String transactionId;

  @ColumnInfo(name = "lao_id", index = true)
  @NonNull
  private String laoId;

  @ColumnInfo(name = "transaction")
  @NonNull
  private TransactionObject transactionObject;

  public TransactionEntity(
      @NonNull String transactionId,
      @NonNull String laoId,
      @NonNull TransactionObject transactionObject) {
    this.transactionId = transactionId;
    this.laoId = laoId;
    this.transactionObject = transactionObject;
  }

  @Ignore
  public TransactionEntity(@NonNull String laoId, @NonNull TransactionObject transactionObject) {
    this(transactionObject.getTransactionId(), laoId, transactionObject);
  }

  @NonNull
  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(@NonNull String transactionId) {
    this.transactionId = transactionId;
  }

  @NonNull
  public String getLaoId() {
    return laoId;
  }

  public void setLaoId(@NonNull String laoId) {
    this.laoId = laoId;
  }

  @NonNull
  public TransactionObject getTransactionObject() {
    return transactionObject;
  }

  public void setTransactionObject(@NonNull TransactionObject transactionObject) {
    this.transactionObject = transactionObject;
  }
}
