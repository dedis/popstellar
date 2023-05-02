package com.github.dedis.popstellar.di;

import android.app.Application;

import androidx.room.Room;

import com.github.dedis.popstellar.repository.database.AppDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AppDatabaseModule {

  private AppDatabaseModule() {
    // Private empty constructor
  }

  @Provides
  @Singleton
  public static AppDatabase provideAppDatabase(Application application) {
    return Room.databaseBuilder(application, AppDatabase.class, "POP-Database")
        .fallbackToDestructiveMigration()
        .allowMainThreadQueries()
        .build();
  }
}
