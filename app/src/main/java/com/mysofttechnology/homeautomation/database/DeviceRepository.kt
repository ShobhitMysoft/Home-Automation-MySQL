package com.mysofttechnology.homeautomation.database

import androidx.lifecycle.LiveData

class DeviceRepository(private val deviceDao: DeviceDao) {

    val readAllData: LiveData<List<Device>> = deviceDao.allDevices()

    suspend fun addDevice(device: Device) {
        deviceDao.addDevice(device)
    }
}