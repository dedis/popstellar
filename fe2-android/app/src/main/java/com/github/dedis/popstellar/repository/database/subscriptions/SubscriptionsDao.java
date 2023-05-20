package com.github.dedis.popstellar.repository.database.subscriptions;

import androidx.room.*;

import io.reactivex.Completable;

@Dao
public interface SubscriptionsDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(SubscriptionsEntity subscriptionsEntity);

  @Query("SELECT * FROM subscriptions WHERE lao_id = :laoId")
  SubscriptionsEntity getSubscriptionsByLao(String laoId);
}
