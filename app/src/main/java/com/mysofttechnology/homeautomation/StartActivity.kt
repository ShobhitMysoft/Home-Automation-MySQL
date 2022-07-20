package com.mysofttechnology.homeautomation

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
    }

    fun isOnline(context: Context = this): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                return true
            }
        }
        return false
    }

    companion object {
        const val APPL1 = "appliance1"
        const val APPL2 = "appliance2"
        const val APPL3 = "appliance3"
        const val APPL4 = "appliance4"
        const val FAN = "fan"
        const val POWER = "power"

        const val SWITCH1 = "switch1"
        const val SWITCH2 = "switch2"
        const val SWITCH3 = "switch3"
        const val SWITCH4 = "switch4"

        const val DEVICES = "devices"
        const val ZERO = "0"
        const val ONE = "1"
        const val ON = "ON"
        const val OFF = "OFF"

        const val BLANK = ""
        const val SWITCH = "switch"
        const val ICON = "icon"
        const val START_TIME = "startTime"
        const val STOP_TIME = "stopTime"
        const val SUN = "sun"
        const val MON = "mon"
        const val TUE = "tue"
        const val WED = "wed"
        const val THU = "thu"
        const val FRI = "fri"
        const val SAT = "sat"
        const val NOTIFICATION = "notification"
    }
}