package com.mysofttechnology.homeautomation.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class Device(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val deviceId: String,
    val bluetoothId: String,
    val s1Name: String,
    val s1Icon: Int,
    val s1State: Int,
    val s2Name: String,
    val s2Icon: Int,
    val s2State: Int,
    val s3Name: String,
    val s3Icon: Int,
    val s3State: Int,
    val s4Name: String,
    val s4Icon: Int,
    val s4State: Int,
//    val s5Name: String,
//    val s5Icon: Int,
    val fanState: Int,
    val fanSpeed: Int,
)
