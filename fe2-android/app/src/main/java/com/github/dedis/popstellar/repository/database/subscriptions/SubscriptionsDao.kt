package com.github.dedis.popstellar.repository.database.subscriptions

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Completable

/** Interface to query the table containing the subscriptions and server address for each lao. */
@Dao
interface SubscriptionsDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insert(subscriptionsEntity: SubscriptionsEntity): Completable

  /**
   * It selects from the table the connection entity for a given lao.
   *
   * @param laoId lao identifier for which retrieving the subscriptions
   */
  @Query("SELECT * FROM subscriptions WHERE lao_id = :laoId")
  fun getSubscriptionsByLao(laoId: String): SubscriptionsEntity?
}
