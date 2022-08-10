package com.github.dedis.popstellar.repository.local;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.Channel;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PersistentData implements Serializable {

  private final List<String> walletSeed;
  private final String serverAddress;
  private final Set<Channel> subscriptions;

  public PersistentData(List<String> walletSeed, String serverAddress, Set<Channel> subscription) {
    if (walletSeed == null || serverAddress == null || subscription == null) {
      throw new IllegalArgumentException("Null argument");
    }
    this.walletSeed = walletSeed.stream().map(String::new).collect(Collectors.toList());
    this.serverAddress = serverAddress;
    this.subscriptions = subscription.stream().map(Channel::new).collect(Collectors.toSet());
  }

  public List<String> getWalletSeed() {
    return walletSeed.stream().map(String::new).collect(Collectors.toList());
  }

  public String getServerAddress() {
    return serverAddress;
  }

  public Set<Channel> getSubscriptions() {
    return subscriptions.stream().map(Channel::new).collect(Collectors.toSet());
  }

  @NonNull
  @Override
  public String toString() {
    return "PersistentData{"
        + "walletSeed="
        + walletSeed
        + ", serverAddress='"
        + serverAddress
        + '\''
        + ", subscriptions="
        + subscriptions
        + '}';
  }
}
