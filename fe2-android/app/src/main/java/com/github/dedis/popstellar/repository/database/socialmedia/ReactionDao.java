package com.github.dedis.popstellar.repository.database.socialmedia;

import androidx.room.*;

import com.github.dedis.popstellar.model.objects.Reaction;
import com.github.dedis.popstellar.model.objects.security.MessageID;

import java.util.List;
import java.util.Set;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public interface ReactionDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(ReactionEntity reactionEntity);

  @Query(
      "SELECT reaction FROM reactions WHERE chirp_id = :chirpId AND reaction_id NOT IN (:filteredIds)")
  Single<List<Reaction>> getReactionsByChirpId(MessageID chirpId, Set<MessageID> filteredIds);
}
