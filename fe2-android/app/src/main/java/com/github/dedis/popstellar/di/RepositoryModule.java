package com.github.dedis.popstellar.di;

import android.app.Application;

import androidx.room.Room;

import com.github.dedis.popstellar.repository.LAODataSource;
import com.github.dedis.popstellar.repository.local.LAODatabase;
import com.github.dedis.popstellar.repository.local.LAOLocalDataSource;
import com.github.dedis.popstellar.utility.scheduler.ProdSchedulerProvider;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;

import javax.inject.Singleton;

import dagger.*;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

  private static final String DATABASE_NAME = "lao_database";

  private RepositoryModule() {}

  @Provides
  @Singleton
  public static LAODatabase provideDatabase(Application application) {
    return Room.databaseBuilder(
            application.getApplicationContext(), LAODatabase.class, DATABASE_NAME)
        .build();
  }

  @Binds
  @Singleton
  public abstract LAODataSource.Local bindsLocal(LAOLocalDataSource localDataSource);

  @Binds
  public abstract SchedulerProvider bindsSchedulerProvider(
      ProdSchedulerProvider prodSchedulerProvider);
}
