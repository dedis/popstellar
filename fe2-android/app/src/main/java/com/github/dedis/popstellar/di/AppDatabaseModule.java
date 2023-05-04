package com.github.dedis.popstellar.di;

import android.app.Application;

import androidx.room.Room;

import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.CustomTypeConverters;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AppDatabaseModule {

  private static final String DATABASE_NAME = "POP-Database";

  private AppDatabaseModule() {
    // Private empty constructor
  }

  @Provides
  @Singleton
  public static AppDatabase provideAppDatabase(Application application) {
    /*
    Injecting the DataRegistry (or the Gson directly) would create a dependency cycle,
    since AppDatabase -> Gson -> DataRegistry -> Handlers -> Repositories -> AppDatabase.
    In order to avoid overcomplicated solutions here it's created a DataRegistry with null handlers,
    as the only function needed is the one to get the object's type for the Gson serializer
     */
    return Room.databaseBuilder(application, AppDatabase.class, DATABASE_NAME)
        .addTypeConverter(
            new CustomTypeConverters(
                JsonModule.provideGson(DataRegistryModule.provideDataRegistryForGson())))
        .fallbackToDestructiveMigration()
        .allowMainThreadQueries()
        .build();
  }
}
