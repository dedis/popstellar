package com.github.dedis.popstellar.repository.database.digitalcash;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject;

@Entity(tableName = "transactions")
@Immutable
public class TransactionEntity {

  @PrimaryKey
  @ColumnInfo(name = "transaction_id")
  @NonNull
  private final String transactionId;

  @ColumnInfo(name = "lao_id", index = true)
  @NonNull
  private final String laoId;

  @ColumnInfo(name = "transaction")
  @NonNull
  private final TransactionObject transactionObject;

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

  @NonNull
  public String getLaoId() {
    return laoId;
  }

  @NonNull
  public TransactionObject getTransactionObject() {
    return transactionObject;
  }
}
