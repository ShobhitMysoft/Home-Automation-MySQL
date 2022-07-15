package com.mysofttechnology.homeautomation.utils

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mysofttechnology.homeautomation.R

class NotificationHelper(base: Context?) : ContextWrapper(base) {

    private var notifManager: NotificationManager

    init {
        notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels()
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createChannels() {
        val channel1 = NotificationChannel(getString(R.string.channel1ID), getString(R.string.channel1Name), NotificationManagerCompat.IMPORTANCE_HIGH)
        channel1.enableLights(true)
        channel1.enableVibration(true)
        channel1.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

        getManager().createNotificationChannel(channel1)
    }

    fun getManager(): NotificationManager {
        if (notifManager == null) {
            notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        return notifManager
    }

    fun getChannel1Notification(title: String, message: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(applicationContext, getString(R.string.channel1ID))
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.mysoft_symbol)
    }
}