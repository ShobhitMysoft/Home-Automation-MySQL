package com.mysofttechnology.homeautomation.database

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class DeviceRepository(private val deviceDao: DeviceDao) {

    val readAllData: Deferred<List<Device>> = GlobalScope.async { deviceDao.allDevices() }

    suspend fun addDevice(device: Device) {
        deviceDao.addDevice(device)
    }

    suspend fun deleteAll() {
        deviceDao.clearTable()
    }
}