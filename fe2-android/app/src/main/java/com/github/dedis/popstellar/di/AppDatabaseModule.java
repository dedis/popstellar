package com.github.dedis.popstellar.di;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    AppDatabase appDatabase =
        Room.databaseBuilder(application, AppDatabase.class, "POP-Database")
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build();
    // Register a callback that closes the db when application stops
    application.registerActivityLifecycleCallbacks(
        new Application.ActivityLifecycleCallbacks() {
          @Override
          public void onActivityCreated(
              @NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            // Nothing
          }

          @Override
          public void onActivityStarted(@NonNull Activity activity) {
            // Nothing
          }

          @Override
          public void onActivityResumed(@NonNull Activity activity) {
            // Nothing
          }

          @Override
          public void onActivityPaused(@NonNull Activity activity) {
            // Nothing
          }

          @Override
          public void onActivityStopped(@NonNull Activity activity) {
            appDatabase.close();
          }

          @Override
          public void onActivitySaveInstanceState(
              @NonNull Activity activity, @NonNull Bundle outState) {
            // Nothing
          }

          @Override
          public void onActivityDestroyed(@NonNull Activity activity) {
            // Nothing
          }
        });
    return appDatabase;
  }
}
