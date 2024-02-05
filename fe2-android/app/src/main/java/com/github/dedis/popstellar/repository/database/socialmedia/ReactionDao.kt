package com.github.dedis.popstellar.repository.database.socialmedia

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.dedis.popstellar.model.objects.Reaction
import com.github.dedis.popstellar.model.objects.security.MessageID
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface ReactionDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insert(reactionEntity: ReactionEntity): Completable

  @Query("SELECT reaction FROM reactions WHERE chirp_id = :chirpId")
  fun getReactionsByChirpId(chirpId: MessageID): Single<List<Reaction>?>
}
