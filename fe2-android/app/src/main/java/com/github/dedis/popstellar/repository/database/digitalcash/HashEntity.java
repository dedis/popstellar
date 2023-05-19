package com.github.dedis.popstellar.repository.database.digitalcash;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.util.Objects;

@Entity(tableName = "hash_dictionary")
public class HashEntity {

  @PrimaryKey
  @ColumnInfo(name = "hash")
  @NonNull
  private String hash;

  @ColumnInfo(name = "public_key")
  @NonNull
  private PublicKey publicKey;

  @ColumnInfo(name = "lao_id", index = true)
  @NonNull
  private String laoId;

  public HashEntity(@NonNull String hash, @NonNull String laoId, @NonNull PublicKey publicKey) {
    this.hash = hash;
    this.laoId = laoId;
    this.publicKey = publicKey;
  }

  @NonNull
  public String getHash() {
    return hash;
  }

  public void setHash(@NonNull String hash) {
    this.hash = hash;
  }

  @NonNull
  public String getLaoId() {
    return laoId;
  }

  public void setLaoId(@NonNull String laoId) {
    this.laoId = laoId;
  }

  @NonNull
  public PublicKey getPublicKey() {
    return publicKey;
  }

  public void setPublicKey(@NonNull PublicKey publicKey) {
    this.publicKey = publicKey;
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
