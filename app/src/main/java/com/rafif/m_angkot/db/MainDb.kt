package com.rafif.m_angkot.db

import android.app.Application
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rafif.m_angkot.network.RouteFromDb

@Database(entities = [RouteFromDb::class], version = 1)
abstract class MainDb : RoomDatabase() {
    abstract val mainDao: MainDao

    companion object {
        @Volatile
        private var db: MainDb? = null

        fun getDb(application: Context): MainDb? {
            if (db == null) {
                synchronized(MainDb::class.java) {
                    if (db == null) {
                        db = Room.databaseBuilder(
                            application.applicationContext,
                            MainDb::class.java, "main_db"
                        ).build()
                    }
                }
            }

            return db
        }
    }
}