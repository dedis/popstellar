package com.github.dedis.popstellar.repository.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.github.dedis.popstellar.repository.local.dao.LAODao;
import com.github.dedis.popstellar.repository.local.entities.LAOEntity;
import com.github.dedis.popstellar.repository.local.entities.LAOWitnessCrossRefEntity;
import com.github.dedis.popstellar.repository.local.entities.MeetingEntity;
import com.github.dedis.popstellar.repository.local.entities.ModificationSignatureEntity;
import com.github.dedis.popstellar.repository.local.entities.PersonEntity;
import com.github.dedis.popstellar.repository.local.entities.RollCallEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
    entities = {
      LAOEntity.class,
      MeetingEntity.class,
      LAOWitnessCrossRefEntity.class,
      RollCallEntity.class,
      PersonEntity.class,
      ModificationSignatureEntity.class
    },
    version = 1)
public abstract class LAODatabase extends RoomDatabase {

  public abstract LAODao laoDao();

  private static volatile LAODatabase INSTANCE;
  private static final int NUMBER_OF_THREADS = 4;
  static final ExecutorService databaseWriteExecutor =
      Executors.newFixedThreadPool(NUMBER_OF_THREADS);
}
