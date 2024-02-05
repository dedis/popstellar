package com.github.dedis.popstellar.di

import android.content.Context
import androidx.room.Room.inMemoryDatabaseBuilder
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.CustomTypeConverters

object AppDatabaseModuleHelper {
  @JvmStatic
  fun getAppDatabase(context: Context): AppDatabase {
    return inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .fallbackToDestructiveMigration()
      .addTypeConverter(
        CustomTypeConverters(
          JsonModule.provideGson(DataRegistryModule.provideDataRegistryForGson())
        )
      )
      .build()
  }
}
