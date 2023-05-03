package com.github.dedis.popstellar.di;

import android.content.Context;

import androidx.room.Room;

import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.CustomTypeConverters;

public class AppDatabaseModuleHelper {

  public static AppDatabase getAppDatabase(Context context) {
    return Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
        .allowMainThreadQueries()
        .fallbackToDestructiveMigration()
        .addTypeConverter(
            new CustomTypeConverters(
                JsonModule.provideGson(DataRegistryModule.provideDataRegistryForGson())))
        .build();
  }
}
