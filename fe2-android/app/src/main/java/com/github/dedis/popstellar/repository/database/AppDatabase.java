package com.github.dedis.popstellar.repository.database;

import androidx.room.*;

import com.github.dedis.popstellar.repository.database.core.CoreDao;
import com.github.dedis.popstellar.repository.database.core.CoreEntity;
import com.github.dedis.popstellar.repository.database.digitalcash.*;
import com.github.dedis.popstellar.repository.database.event.election.ElectionDao;
import com.github.dedis.popstellar.repository.database.event.election.ElectionEntity;
import com.github.dedis.popstellar.repository.database.event.meeting.MeetingDao;
import com.github.dedis.popstellar.repository.database.event.meeting.MeetingEntity;
import com.github.dedis.popstellar.repository.database.event.rollcall.RollCallDao;
import com.github.dedis.popstellar.repository.database.event.rollcall.RollCallEntity;
import com.github.dedis.popstellar.repository.database.lao.LAODao;
import com.github.dedis.popstellar.repository.database.lao.LAOEntity;
import com.github.dedis.popstellar.repository.database.message.MessageDao;
import com.github.dedis.popstellar.repository.database.message.MessageEntity;
import com.github.dedis.popstellar.repository.database.socialmedia.*;
import com.github.dedis.popstellar.repository.database.witnessing.WitnessingDao;
import com.github.dedis.popstellar.repository.database.witnessing.WitnessingEntity;

import javax.inject.Singleton;

@Singleton
@Database(
    entities = {
      MessageEntity.class,
      LAOEntity.class,
      CoreEntity.class,
      ElectionEntity.class,
      RollCallEntity.class,
      MeetingEntity.class,
      ChirpEntity.class,
      ReactionEntity.class,
      TransactionEntity.class,
      HashEntity.class,
      WitnessingEntity.class
    },
    version = 1)
@TypeConverters(CustomTypeConverters.class)
public abstract class AppDatabase extends RoomDatabase {
  public abstract MessageDao messageDao();

  public abstract LAODao laoDao();

  public abstract CoreDao coreDao();

  public abstract WitnessingDao witnessDao();

  public abstract ElectionDao electionDao();

  public abstract RollCallDao rollCallDao();

  public abstract MeetingDao meetingDao();

  public abstract ChirpDao chirpDao();

  public abstract ReactionDao reactionDao();

  public abstract TransactionDao transactionDao();

  public abstract HashDao hashDao();
}
