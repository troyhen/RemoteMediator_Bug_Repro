package com.troy.remotemediatorbugrepro

import android.app.Application
import androidx.room.Room

class App : Application() {

    val backend = ContentApi
    val db by lazy { Room.databaseBuilder(applicationContext, AppDatabase::class.java, "app-database").build() }

    init {
        app = this
    }

    companion object {
        lateinit var app: App
    }
}