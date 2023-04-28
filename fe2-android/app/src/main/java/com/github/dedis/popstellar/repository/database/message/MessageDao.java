package com.github.dedis.popstellar.repository.database.message;

import androidx.room.*;

import com.github.dedis.popstellar.model.objects.security.MessageID;

import io.reactivex.Completable;

@Dao
public interface MessageDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(MessageEntity message);

  @Query("SELECT * FROM messages WHERE message_id = :messageId")
  MessageEntity getMessageById(MessageID messageId);
}
