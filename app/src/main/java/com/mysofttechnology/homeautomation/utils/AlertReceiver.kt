package com.mysofttechnology.homeautomation.utils

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.mysofttechnology.homeautomation.StartActivity
import com.mysofttechnology.homeautomation.StartActivity.Companion.DEVICES
import com.mysofttechnology.homeautomation.StartActivity.Companion.ONE
import com.mysofttechnology.homeautomation.StartActivity.Companion.SWITCH1
import com.mysofttechnology.homeautomation.StartActivity.Companion.SWITCH2
import com.mysofttechnology.homeautomation.StartActivity.Companion.SWITCH3
import com.mysofttechnology.homeautomation.StartActivity.Companion.ZERO
import com.mysofttechnology.homeautomation.activities.EditSwitchActivity
import com.mysofttechnology.homeautomation.activities.EditSwitchActivity.Companion.START

private const val TAG = "AlertReceiver"
class AlertReceiver : BroadcastReceiver() {
    private lateinit var intent: Intent
    private lateinit var context: Context

    private lateinit var roomId: String
    private lateinit var switchId: String

    private var myDB: MyFirebaseDatabase = MyFirebaseDatabase()

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive: Requested")
        Toast.makeText(context, "Alert requested.", Toast.LENGTH_SHORT).show()
        this.intent = intent!!
        this.context = context!!

        roomId = intent.getStringExtra(EditSwitchActivity.ROOM_ID)!!
        switchId = intent.getStringExtra(EditSwitchActivity.SWITCH_ID)!!

        toggleSwitch()
    }

    private fun toggleSwitch() {
        notifyUser("Requesting to perform task...")
        val switch = when (switchId) {
            SWITCH1 -> "appliance1"
            SWITCH2 -> "appliance2"
            SWITCH3 -> "appliance3"
            else -> "appliance4"
        }
        myDB.dbDevicesRef.child(roomId).child(switch).setValue(if (intent.action == START) ONE else ZERO).addOnSuccessListener {
            myDB.dbProfileRef.child(DEVICES).child(roomId).get().addOnSuccessListener { room ->
                val roomName = room.child("name").value
                val switchName = room.child(switchId).child("name").value
                var msg = "$switchName of $roomName is turned OFF"
                if (intent.action == START) msg = "$switchName of $roomName is turned ON"
                notifyUser(msg)
            }.addOnFailureListener { notifyUser("Automation task completed.") }
        }.addOnFailureListener { notifyUser("Failed to perform automation task.") }
    }

    private fun notifyUser(msg: String) {
        val pendingIntent = PendingIntent.getActivity(context, 0, Intent(context, StartActivity::class.java), 0)
        val notificationHelper = NotificationHelper(context)
        val notBuilder: NotificationCompat.Builder =
            notificationHelper.getChannel1Notification("Smart Home", msg)
        notBuilder.setContentIntent(pendingIntent)
        notificationHelper.getManager().notify(1, notBuilder.build())
    }
}