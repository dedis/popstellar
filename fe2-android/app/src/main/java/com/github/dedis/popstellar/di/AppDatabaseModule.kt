package com.github.dedis.popstellar.di

import android.app.Application
import androidx.room.Room.databaseBuilder
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.CustomTypeConverters
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppDatabaseModule {
    private const val DATABASE_NAME = "POP-Database"

    @JvmStatic
    @Provides
    @Singleton
    fun provideAppDatabase(application: Application?): AppDatabase {
        /*
        Injecting the DataRegistry (or the Gson directly) would create a dependency cycle,
        since AppDatabase -> Gson -> DataRegistry -> Handlers -> Repositories -> AppDatabase.
        In order to avoid overcomplicated solutions here it's created a DataRegistry with null handlers,
        as the only function needed is the one to get the object's type for the Gson serializer
        */
        return databaseBuilder(application!!, AppDatabase::class.java, DATABASE_NAME)
                .addTypeConverter(
                        CustomTypeConverters(
                                JsonModule.provideGson(DataRegistryModule.provideDataRegistryForGson())))
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()
    }
}