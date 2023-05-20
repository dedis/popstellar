package com.github.dedis.popstellar.repository.database.wallet;

import androidx.annotation.NonNull;
import androidx.room.*;

import java.util.*;

@Entity(tableName = "wallet")
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WalletEntity that = (WalletEntity) o;
    return walletSeed.equals(that.walletSeed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(walletSeed);
  }

  @NonNull
  @Override
  public String toString() {
    return "PersistentData{" + "walletSeed=" + walletSeed + '}';
  }
}
