package com.mysofttechnology.homeautomation

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class ExitAppDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Exit").setMessage("Are you sure you want to exit?")
                .setPositiveButton("Ok"
                ) { _, _ ->
                    activity?.finish()
                }
                .setNegativeButton("No") { _, _ -> }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}