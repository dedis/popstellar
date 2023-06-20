package com.github.dedis.popstellar.repository.database.wallet;

import androidx.room.*;

import io.reactivex.Completable;

/** Interface to query the table containing the wallet seed */
@Dao
public interface WalletDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(WalletEntity walletEntity);

  @Query("SELECT * FROM wallet LIMIT 1")
  WalletEntity getWallet();
}
