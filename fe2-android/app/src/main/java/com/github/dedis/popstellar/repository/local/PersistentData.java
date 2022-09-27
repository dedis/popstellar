package com.github.dedis.popstellar.repository.local;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.Channel;

import java.io.Serializable;
import java.util.*;

public class PersistentData implements Serializable {

  private final List<String> walletSeed;
  private final String serverAddress;
  private final Set<Channel> subscriptions;

  public PersistentData(@NonNull String[] walletSeed, @NonNull String serverAddress, @NonNull Set<Channel> subscription) {
    this.walletSeed = Collections.unmodifiableList(Arrays.asList(walletSeed));
    this.serverAddress = serverAddress;
    this.subscriptions = new HashSet<>(subscription);
  }

  public String[] getWalletSeed() {
    return walletSeed.toArray(new String[0]);
  }

  public String getServerAddress() {
    return serverAddress;
  }

  public Set<Channel> getSubscriptions() {
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
