package com.github.dedis.popstellar.repository.database;

import androidx.room.*;

import com.github.dedis.popstellar.repository.database.core.CoreDao;
import com.github.dedis.popstellar.repository.database.core.CoreEntity;
import com.github.dedis.popstellar.repository.database.lao.LAODao;
import com.github.dedis.popstellar.repository.database.lao.LAOEntity;
import com.github.dedis.popstellar.repository.database.message.MessageDao;
import com.github.dedis.popstellar.repository.database.message.MessageEntity;

import javax.inject.Singleton;

@Singleton
@Database(
    entities = {MessageEntity.class, LAOEntity.class, CoreEntity.class},
    version = 4)
@TypeConverters(CustomTypeConverters.class)
public abstract class AppDatabase extends RoomDatabase {
  public abstract MessageDao messageDao();

  public abstract LAODao laoDao();

  public abstract CoreDao coreDao();
}
