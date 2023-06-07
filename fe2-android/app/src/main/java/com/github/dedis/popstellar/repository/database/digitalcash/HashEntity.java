package com.github.dedis.popstellar.repository.database.digitalcash;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.util.Objects;

@Entity(tableName = "hash_dictionary")
@Immutable
public class HashEntity {

  @PrimaryKey
  @ColumnInfo(name = "hash")
  @NonNull
  private final String hash;

  @ColumnInfo(name = "public_key")
  @NonNull
  private final PublicKey publicKey;

  @ColumnInfo(name = "lao_id", index = true)
  @NonNull
  private final String laoId;

  public HashEntity(@NonNull String hash, @NonNull String laoId, @NonNull PublicKey publicKey) {
    this.hash = hash;
    this.laoId = laoId;
    this.publicKey = publicKey;
  }

  @NonNull
  public String getHash() {
    return hash;
  }

  @NonNull
  public String getLaoId() {
    return laoId;
  }

  @NonNull
  public PublicKey getPublicKey() {
    return publicKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HashEntity that = (HashEntity) o;
    return hash.equals(that.hash) && publicKey.equals(that.publicKey) && laoId.equals(that.laoId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hash, publicKey, laoId);
  }
}
