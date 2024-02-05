package com.github.dedis.popstellar.repository.database.message

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.dedis.popstellar.model.objects.security.MessageID
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface MessageDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE) fun insert(message: MessageEntity): Completable

  @Query("SELECT * FROM messages WHERE message_id = :messageId")
  fun getMessageById(messageId: MessageID): MessageEntity?

  @Query("SELECT * FROM messages LIMIT :n")
  fun takeFirstNMessages(n: Int): Single<List<MessageEntity>?>
}
