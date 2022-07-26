package com.mysofttechnology.homeautomation.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.mysofttechnology.homeautomation.database.Device
import com.mysofttechnology.homeautomation.database.DeviceRepository
import com.mysofttechnology.homeautomation.database.DevicesDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeviceViewModel(application: Application): AndroidViewModel(application) {

    val readAllData: LiveData<List<Device>>
    private val repository: DeviceRepository

    init {
        val deviceDao = DevicesDb.getDatabase(application).deviceDao()
        repository = DeviceRepository(deviceDao)
        readAllData = repository.readAllData
    }

    fun addDevice(device: Device) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addDevice(device)
        }
    }
}