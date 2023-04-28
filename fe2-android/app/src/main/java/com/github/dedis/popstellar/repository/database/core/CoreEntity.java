package com.github.dedis.popstellar.repository.database.core;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.objects.Channel;

import java.util.*;

@Entity(tableName = "core")
public class CoreEntity {

  @PrimaryKey(autoGenerate = true)
  private int id;

  @ColumnInfo(name = "server_address")
  @NonNull
  private String serverAddress;

  @ColumnInfo(name = "wallet_seed")
  @NonNull
  private List<String> walletSeed;

  @NonNull private Set<Channel> subscriptions;

  public CoreEntity(
      @NonNull String serverAddress,
      @NonNull List<String> walletSeed,
      @NonNull Set<Channel> subscriptions) {
    this.walletSeed = walletSeed;
    this.serverAddress = serverAddress;
    this.subscriptions = new HashSet<>(subscriptions);
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @NonNull
  public String getServerAddress() {
    return serverAddress;
  }

  public void setServerAddress(@NonNull String serverAddress) {
    this.serverAddress = serverAddress;
  }

  @NonNull
  public List<String> getWalletSeed() {
    return walletSeed;
  }

  public void setWalletSeed(@NonNull List<String> walletSeed) {
    this.walletSeed = walletSeed;
  }

  @NonNull
  public Set<Channel> getSubscriptions() {
    return subscriptions;
  }

  public void setSubscriptions(@NonNull Set<Channel> subscriptions) {
    this.subscriptions = subscriptions;
  }

  public String[] getWalletSeedArray() {
    return walletSeed.toArray(new String[0]);
  }

  public Set<Channel> getSubscriptionsCopy() {
    return new HashSet<>(subscriptions);
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
