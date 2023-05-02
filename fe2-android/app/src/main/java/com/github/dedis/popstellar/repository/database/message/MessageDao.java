package com.github.dedis.popstellar.repository.database.message;

import androidx.room.*;

import com.github.dedis.popstellar.model.objects.security.MessageID;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public interface MessageDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(MessageEntity message);

  @Query("SELECT * FROM messages WHERE message_id = :messageId")
  MessageEntity getMessageById(MessageID messageId);

  @Query("SELECT * FROM messages LIMIT :n")
  Single<List<MessageEntity>> takeFirstNMessages(int n);
}
