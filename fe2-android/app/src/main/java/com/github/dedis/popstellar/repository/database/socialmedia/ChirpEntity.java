package com.github.dedis.popstellar.repository.database.socialmedia;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.security.MessageID;

@Entity(tableName = "chirps")
public class ChirpEntity {

  @PrimaryKey
  @ColumnInfo(name = "chirp_id")
  @NonNull
  private MessageID chirpId;

  @ColumnInfo(name = "lao_id", index = true)
  @NonNull
  private String laoId;

  @ColumnInfo(name = "chirp")
  @NonNull
  private Chirp chirp;

  public ChirpEntity(@NonNull MessageID chirpId, @NonNull String laoId, @NonNull Chirp chirp) {
    this.chirpId = chirpId;
    this.laoId = laoId;
    this.chirp = chirp;
  }

  @Ignore
  public ChirpEntity(@NonNull String laoId, @NonNull Chirp chirp) {
    this(chirp.getId(), laoId, chirp);
  }

  @NonNull
  public MessageID getChirpId() {
    return chirpId;
  }

  public void setChirpId(@NonNull MessageID chirpId) {
    this.chirpId = chirpId;
  }

  @NonNull
  public String getLaoId() {
    return laoId;
  }

  public void setLaoId(@NonNull String laoId) {
    this.laoId = laoId;
  }

  @NonNull
  public Chirp getChirp() {
    return chirp;
  }

  public void setChirp(@NonNull Chirp chirp) {
    this.chirp = chirp;
  }
}
