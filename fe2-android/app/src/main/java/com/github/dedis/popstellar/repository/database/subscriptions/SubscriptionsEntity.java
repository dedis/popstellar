package com.github.dedis.popstellar.repository.database.subscriptions;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.objects.Channel;

import java.util.Set;

@Entity(tableName = "subscriptions")
public class SubscriptionsEntity {

  @PrimaryKey
  @ColumnInfo(name = "lao_id")
  @NonNull
  private final String laoId;

  @ColumnInfo(name = "server_address")
  @NonNull
  private final String serverAddress;

  @ColumnInfo(name = "subscription")
  @NonNull
  private final Set<Channel> subscriptions;

  public SubscriptionsEntity(
      @NonNull String laoId, @NonNull String serverAddress, @NonNull Set<Channel> subscriptions) {
    this.laoId = laoId;
    this.serverAddress = serverAddress;
    this.subscriptions = subscriptions;
  }

  @NonNull
  public String getLaoId() {
    return laoId;
  }

  @NonNull
  public String getServerAddress() {
    return serverAddress;
  }

  @NonNull
  public Set<Channel> getSubscriptions() {
    return subscriptions;
  }
}
