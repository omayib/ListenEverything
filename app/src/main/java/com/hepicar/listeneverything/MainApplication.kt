package com.hepicar.listeneverything

import android.app.Application
import android.arch.persistence.room.Room
import com.hepicar.listeneverything.repository.AppDatabase

class MainApplication : Application(){
    var db:AppDatabase? = null

    override fun onCreate() {
        super.onCreate()

    }
}