package com.mysofttechnology.homeautomation.utils

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.mysofttechnology.homeautomation.activities.ErrorActivity

private const val TAG = "MyApp"
class MyApp: Application(), ExceptionListener {
    override fun onCreate() {
        super.onCreate()

        setupExceptionHandler()
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Log.e(TAG, "uncaughtException: ${throwable.message}")
        /*val intent = Intent(applicationContext, ErrorActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)*/
    }

    private fun setupExceptionHandler() {
        Handler(Looper.getMainLooper()).post {
            while (true) {
                try {
                    Looper.loop()
                } catch (t: Throwable) {
                    uncaughtException(Looper.getMainLooper().thread, t)
                }
            }
        }
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            uncaughtException(t, e)
        }
    }
}