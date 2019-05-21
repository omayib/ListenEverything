package com.hepicar.listeneverything.repository

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.Room
import android.content.Context


@Database(entities = arrayOf(SensorData::class), version = 1)
abstract class AppDatabase : RoomDatabase() {
    private var INSTANCE: AppDatabase? = null


    fun getAppDatabase(context: Context): AppDatabase {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase::class.java, "/storage/emulated/0/sensor-database")
                // allow queries on the main thread.
                // Don't do this on a real app! See PersistenceBasicSample for an example.
                .allowMainThreadQueries()
                .build()
        }
        return INSTANCE as AppDatabase
    }

    fun destroyInstance() {
        INSTANCE = null
    }
}