package com.github.dedis.popstellar.di;

import android.content.Context;

import androidx.room.Room;

import com.github.dedis.popstellar.repository.database.AppDatabase;

public class AppDatabaseModuleHelper {
  private static AppDatabase appDatabase;

  public static AppDatabase getAppDatabase(Context context) {
    if (appDatabase == null) {
      appDatabase =
          Room.inMemoryDatabaseBuilder(context, AppDatabase.class).allowMainThreadQueries().build();
    }
    return appDatabase;
  }
}
