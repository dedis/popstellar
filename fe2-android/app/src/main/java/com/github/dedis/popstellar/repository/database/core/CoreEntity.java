package com.github.dedis.popstellar.repository.database.core;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.objects.Channel;

import java.util.*;

@Entity(tableName = "core")
public class CoreEntity {

  @ColumnInfo(name = "id")
  @PrimaryKey
  private final int id;

  @ColumnInfo(name = "server_address")
  @NonNull
  private final String serverAddress;

  @ColumnInfo(name = "wallet_seed")
  @NonNull
  private final List<String> walletSeed;

  @ColumnInfo(name = "subscription")
  @NonNull
  private final Set<Channel> subscriptions;

  public CoreEntity(
      int id,
      @NonNull String serverAddress,
      @NonNull List<String> walletSeed,
      @NonNull Set<Channel> subscriptions) {
    this.id = id;
    this.serverAddress = serverAddress;
    this.walletSeed = walletSeed;
    this.subscriptions = subscriptions;
  }

  public int getId() {
    return id;
  }

  @NonNull
  public String getServerAddress() {
    return serverAddress;
  }

  @NonNull
  public List<String> getWalletSeed() {
    return walletSeed;
  }

  @NonNull
  public Set<Channel> getSubscriptions() {
    return subscriptions;
  }

  public String[] getWalletSeedArray() {
    return walletSeed.toArray(new String[0]);
  }

  public Set<Channel> getSubscriptionsCopy() {
    return new HashSet<>(subscriptions);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CoreEntity that = (CoreEntity) o;
    return serverAddress.equals(that.serverAddress)
        && walletSeed.equals(that.walletSeed)
        && subscriptions.equals(that.subscriptions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serverAddress, walletSeed, subscriptions);
  }

  @NonNull
  @Override
  public String toString() {
    return "PersistentData{"
        + "walletSeed size="
        + walletSeed.size()
        + ", serverAddress='"
        + serverAddress
        + '\''
        + ", subscriptions="
        + subscriptions
        + '}';
  }
}
