package com.github.dedis.popstellar.repository.database.socialmedia;

import androidx.annotation.NonNull;
import androidx.room.*;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.objects.Reaction;
import com.github.dedis.popstellar.model.objects.security.MessageID;

@Entity(tableName = "reactions")
@Immutable
public class ReactionEntity {

  @PrimaryKey
  @ColumnInfo(name = "reaction_id")
  @NonNull
  private final MessageID reactionId;

  @ColumnInfo(name = "chirp_id", index = true)
  @NonNull
  private final MessageID chirpId;

  @ColumnInfo(name = "reaction")
  @NonNull
  private final Reaction reaction;

  public ReactionEntity(
      @NonNull MessageID reactionId, @NonNull MessageID chirpId, @NonNull Reaction reaction) {
    this.reactionId = reactionId;
    this.chirpId = chirpId;
    this.reaction = reaction;
  }

  @Ignore
  public ReactionEntity(@NonNull Reaction reaction) {
    this(reaction.getId(), reaction.getChirpId(), reaction);
  }

  @NonNull
  public MessageID getReactionId() {
    return reactionId;
  }

  @NonNull
  public MessageID getChirpId() {
    return chirpId;
  }

  @NonNull
  public Reaction getReaction() {
    return reaction;
  }
}
