package com.mysofttechnology.homeautomation

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment

class LoadingDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val view = requireActivity().layoutInflater.inflate(R.layout.loading_dialog_layout, null)

        return activity?.let {
            builder.setView(view).setCancelable(false)

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}