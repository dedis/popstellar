package com.github.dedis.popstellar.repository.database.wallet;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.Immutable;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "wallet")
@Immutable
public class WalletEntity {

  // The id is used to keep always at most one entry in the database
  @PrimaryKey
  @ColumnInfo(name = "id")
  private final int id;

  @ColumnInfo(name = "wallet_seed")
  @NonNull
  private final List<String> walletSeed;

  public WalletEntity(int id, @NonNull List<String> walletSeed) {
    this.id = id;
    this.walletSeed = walletSeed;
  }

  public int getId() {
    return id;
  }

  @NonNull
  public List<String> getWalletSeed() {
    return new ArrayList<>(walletSeed);
  }

  public String[] getWalletSeedArray() {
    return walletSeed.toArray(new String[0]);
  }
}
