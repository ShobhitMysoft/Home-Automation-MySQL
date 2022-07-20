package com.mysofttechnology.homeautomation.activities

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import com.mysofttechnology.homeautomation.R
import com.mysofttechnology.homeautomation.StartActivity

class SplashScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        supportActionBar?.hide()

        Handler().postDelayed({
            startActivity(Intent(this, StartActivity::class.java))
            finish()
        }, 3000)
    }
}