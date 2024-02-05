package com.github.dedis.popstellar.repository.database.digitalcash

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.security.PublicKey
import java.util.Objects

@Entity(tableName = "hash_dictionary")
@Immutable
class HashEntity(
    @field:ColumnInfo(name = "hash") @field:PrimaryKey val hash: String,
    @field:ColumnInfo(name = "lao_id", index = true) val laoId: String,
    @field:ColumnInfo(name = "public_key") val publicKey: PublicKey
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as HashEntity
    return hash == that.hash && publicKey == that.publicKey && laoId == that.laoId
  }

  override fun hashCode(): Int {
    return Objects.hash(hash, publicKey, laoId)
  }
}
