package com.github.dedis.popstellar.di;

import com.github.dedis.popstellar.utility.scheduler.ProdSchedulerProvider;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

  private static final String DATABASE_NAME = "lao_database";

  private RepositoryModule() {}

  @Binds
  public abstract SchedulerProvider bindsSchedulerProvider(
      ProdSchedulerProvider prodSchedulerProvider);
}
