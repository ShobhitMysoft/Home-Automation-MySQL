package com.mysofttechnology.homeautomation.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.mysofttechnology.homeautomation.R
import com.mysofttechnology.homeautomation.StartActivity

class WorkDoneActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_work_done)

        val continueBtn = findViewById<Button>(R.id.wd_continue_btn)

        continueBtn.setOnClickListener { goToStartActivity() }
    }

    private fun goToStartActivity() {
        val intent = Intent(this, StartActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        goToStartActivity()
    }
}