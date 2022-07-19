package com.mysofttechnology.homeautomation.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.mysofttechnology.homeautomation.models.RoomsViewModel

private const val TAG = "MyFirebaseDatabase"
class MyFirebaseDatabase {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
    var dbRootRef: DatabaseReference
    var dbDevicesRef: DatabaseReference
    var dbUsersRef: DatabaseReference

    var dbProfileRef: DatabaseReference

    private var currentUser: FirebaseUser? = auth.currentUser
    private var cuPhoneNo: String? = currentUser?.phoneNumber.toString().takeLast(10)

    init {
        currentUser = auth.currentUser
        cuPhoneNo = currentUser?.phoneNumber.toString().takeLast(10)

        dbRootRef = db.getReference("root/")
        dbDevicesRef = db.getReference("root/devices")
        dbUsersRef = db.getReference("root/Users")

        dbProfileRef = db.getReference("root/users/$cuPhoneNo/profile")
    }

    fun removeRoom(roomName: String) {
        dbProfileRef = db.getReference("root/users/$cuPhoneNo/profile")

        dbProfileRef.get().addOnSuccessListener {
            var deviceOrder: String? = null
            it.child("devices").children.forEach { device ->
                if (device.child("name").value == roomName) {
                    Log.d(TAG, "removeRoom: ${device.key}")
                    deviceOrder = device.child("order").value.toString()
                    dbProfileRef.child("devices").child("${device.key}").removeValue()
                }
            }

//            var deviceCount = (it.child("deviceCount").value as String).toInt()
//
//            if (deviceCount > 1) {
//                deviceCount--
//                dbProfileRef.child("deviceCount").setValue(deviceCount.toString())
//            } else dbProfileRef.child("devices").removeValue()

            if (it.child("devices").childrenCount < 1) dbProfileRef.child("devices").removeValue()

            if (!deviceOrder.isNullOrBlank()) updateOrder(deviceOrder!!)
        }
    }

    fun updateOrder(deviceOrder: String) {
        Log.d(TAG, "updateOrder: Ordering rooms...")
        /*
        * 1. get device list by order
        * 2. check index of the deleted device using deviceOrder var
        * 3. check if list(index).hasNext()
        * 4 .loop and device.order-1 */
    }

}