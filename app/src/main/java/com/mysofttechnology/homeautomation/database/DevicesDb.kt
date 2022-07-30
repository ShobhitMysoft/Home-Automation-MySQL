package com.mysofttechnology.homeautomation.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Device::class], version = 1, exportSchema = false)
abstract class DevicesDb: RoomDatabase() {

    abstract fun deviceDao(): DeviceDao

    companion object {
        @Volatile
        private var INSTANCE: DevicesDb? = null

        fun getDatabase(context: Context): DevicesDb {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DevicesDb::class.java,
                    "devices"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}