package com.github.dedis.popstellar.repository.database.subscriptions;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.objects.Channel;

import java.util.Objects;
import java.util.Set;

@Entity(tableName = "subscriptions")
public class SubscriptionsEntity {

  @PrimaryKey
  @ColumnInfo(name = "lao_id")
  @NonNull
  private String laoId;

  @ColumnInfo(name = "server_address")
  @NonNull
  private String serverAddress;

  @ColumnInfo(name = "subscription")
  @NonNull
  private Set<Channel> subscriptions;

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

  public void setLaoId(@NonNull String laoId) {
    this.laoId = laoId;
  }

  public void setServerAddress(@NonNull String serverAddress) {
    this.serverAddress = serverAddress;
  }

  public void setSubscriptions(@NonNull Set<Channel> subscriptions) {
    this.subscriptions = subscriptions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubscriptionsEntity that = (SubscriptionsEntity) o;
    return laoId.equals(that.laoId)
        && serverAddress.equals(that.serverAddress)
        && subscriptions.equals(that.subscriptions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(laoId, serverAddress, subscriptions);
  }
}
