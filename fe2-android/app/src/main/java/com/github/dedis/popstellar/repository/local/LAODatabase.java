package com.github.dedis.popstellar.repository.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.github.dedis.popstellar.repository.local.dao.LAODao;
import com.github.dedis.popstellar.repository.local.entities.LAO;
import com.github.dedis.popstellar.repository.local.entities.LAOWitnessCrossRef;
import com.github.dedis.popstellar.repository.local.entities.Meeting;
import com.github.dedis.popstellar.repository.local.entities.ModificationSignature;
import com.github.dedis.popstellar.repository.local.entities.Person;
import com.github.dedis.popstellar.repository.local.entities.RollCall;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
    entities = {
        LAO.class,
        Meeting.class,
        LAOWitnessCrossRef.class,
        RollCall.class,
        Person.class,
        ModificationSignature.class
    },
    version = 1,
    exportSchema = true)
public abstract class LAODatabase extends RoomDatabase {

  private static final String DATABASE_NAME = "lao_database";

  public abstract LAODao laoDao();

  private static volatile LAODatabase INSTANCE;
  private static final int NUMBER_OF_THREADS = 4;
  static final ExecutorService databaseWriteExecutor =
      Executors.newFixedThreadPool(NUMBER_OF_THREADS);

  public static LAODatabase getDatabase(final Context context) {
    if (INSTANCE == null) {
      synchronized (LAODatabase.class) {
        if (INSTANCE == null) {
          INSTANCE =
              Room.databaseBuilder(
                  context.getApplicationContext(), LAODatabase.class, DATABASE_NAME)
                  .build();
        }
      }
    }
    return INSTANCE;
  }
}
