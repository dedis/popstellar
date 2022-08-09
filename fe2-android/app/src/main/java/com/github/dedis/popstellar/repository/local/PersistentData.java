package com.github.dedis.popstellar.repository.local;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.Channel;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public class PersistentData implements Serializable {

  private final String walletSeed;
  private final String serverAddress;
  private final List<Channel> channels;

  public PersistentData(String walletSeed, String serverAddress, List<Channel> channels) {
    if (walletSeed == null || serverAddress == null || channels == null) {
      throw new IllegalArgumentException("Null argument");
    }
    this.walletSeed = walletSeed;
    this.serverAddress = serverAddress;
    this.channels = channels.stream().map(Channel::new).collect(Collectors.toList());
  }

  public String getWalletSeed() {
    return walletSeed;
  }

  public String getServerAddress() {
    return serverAddress;
  }

  public List<Channel> getChannels() {
    return channels.stream().map(Channel::new).collect(Collectors.toList());
  }

  @NonNull
  @Override
  public String toString() {
    // The wallet seed should not be in logs
    return "PersistentData{"
        + "serverAddress='"
        + serverAddress
        + '\''
        + ", channels="
        + channels
        + '}';
  }
}
