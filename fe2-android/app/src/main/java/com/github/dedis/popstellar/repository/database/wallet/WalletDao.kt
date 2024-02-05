package com.github.dedis.popstellar.repository.database.wallet

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Completable

/** Interface to query the table containing the wallet seed */
@Dao
interface WalletDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insert(walletEntity: WalletEntity): Completable

  @get:Query("SELECT * FROM wallet LIMIT 1") val wallet: WalletEntity?
}
