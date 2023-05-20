package com.github.dedis.popstellar.repository.database.subscriptions;

import androidx.room.*;

import io.reactivex.Completable;

/** Interface to query the table containing the subscriptions and server address for each lao. */
@Dao
public interface SubscriptionsDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(SubscriptionsEntity subscriptionsEntity);

  /**
   * It selects from the table the connection entity for a given lao.
   *
   * @param laoId lao identifier for which retrieving the subscriptions
   */
  @Query("SELECT * FROM subscriptions WHERE lao_id = :laoId")
  SubscriptionsEntity getSubscriptionsByLao(String laoId);
}
