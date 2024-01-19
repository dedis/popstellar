package com.github.dedis.popstellar.repository.database.wallet

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.dedis.popstellar.model.Immutable

@Entity(tableName = "wallet")
@Immutable
class WalletEntity(
    // The id is used to keep always at most one entry in the database
    @field:ColumnInfo(name = "id") @field:PrimaryKey val id: Int,
    @field:ColumnInfo(name = "wallet_seed") private val walletSeed: List<String>
) {

  fun getWalletSeed(): List<String> {
    return ArrayList(walletSeed)
  }

  val walletSeedArray: Array<String>
    get() = walletSeed.toTypedArray()
}
