package com.mysofttechnology.homeautomation.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Device::class], version = 2, exportSchema = false)
abstract class DevicesDb : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao

    companion object {

        private val migration_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE devices ADD COLUMN switchCount TEXT NOT NULL DEFAULT(0)")
                database.execSQL(
                    "ALTER TABLE devices ADD COLUMN s6Name TEXT NOT NULL DEFAULT(0)")
                database.execSQL(
                    "ALTER TABLE devices ADD COLUMN s6Icon INTEGER NOT NULL DEFAULT(0)")
                database.execSQL(
                    "ALTER TABLE devices ADD COLUMN s6State INTEGER NOT NULL DEFAULT(0)")
            }

        }

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
                )
                    .addMigrations(migration_1_2)
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}