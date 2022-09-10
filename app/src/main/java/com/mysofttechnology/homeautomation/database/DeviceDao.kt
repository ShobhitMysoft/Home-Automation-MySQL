package com.mysofttechnology.homeautomation.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DeviceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addDevice(device: Device)

    @Query("SELECT * FROM devices ORDER BY id ASC")
    suspend fun allDevices(): List<Device>

    @Query("DELETE FROM devices")
    fun clearTable()
}