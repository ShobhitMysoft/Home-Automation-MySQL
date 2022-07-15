package com.mysofttechnology.homeautomation.activities

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.mysofttechnology.homeautomation.LoadingDialog
import com.mysofttechnology.homeautomation.R
import com.mysofttechnology.homeautomation.StartActivity
import com.mysofttechnology.homeautomation.StartActivity.Companion.DEVICES
import com.mysofttechnology.homeautomation.StartActivity.Companion.FRI
import com.mysofttechnology.homeautomation.StartActivity.Companion.MON
import com.mysofttechnology.homeautomation.StartActivity.Companion.ONE
import com.mysofttechnology.homeautomation.StartActivity.Companion.SAT
import com.mysofttechnology.homeautomation.StartActivity.Companion.START_TIME
import com.mysofttechnology.homeautomation.StartActivity.Companion.STOP_TIME
import com.mysofttechnology.homeautomation.StartActivity.Companion.SUN
import com.mysofttechnology.homeautomation.StartActivity.Companion.THU
import com.mysofttechnology.homeautomation.StartActivity.Companion.TUE
import com.mysofttechnology.homeautomation.StartActivity.Companion.WED
import com.mysofttechnology.homeautomation.StartActivity.Companion.ZERO
import com.mysofttechnology.homeautomation.adapters.IconListAdapter
import com.mysofttechnology.homeautomation.databinding.ActivityEditSwitchBinding
import com.mysofttechnology.homeautomation.utils.AlertReceiver
import com.mysofttechnology.homeautomation.utils.MyFirebaseDatabase
import com.mysofttechnology.homeautomation.utils.TimePickerFragment
import java.text.DateFormat
import java.util.*

private const val TAG = "EditSwitchActivity"

class EditSwitchActivity : AppCompatActivity(), TimePickerDialog.OnTimeSetListener {
    private lateinit var cal1: Calendar
    private lateinit var cal2: Calendar
    private lateinit var bind: ActivityEditSwitchBinding

    private var START_TIME_FLAG = false
    private var STOP_TIME_FLAG = false
    private var TIME_PICKER_FLAG = 1
    private var START_REQUEST_CODE = 10
    private var STOP_REQUEST_CODE = 20
    private lateinit var roomId: String
    private lateinit var switchId: String

    private var switchIconIndex: Int = 0
    private lateinit var iconsList: TypedArray
    private lateinit var iconsNameList: Array<String>

    private lateinit var myDB: MyFirebaseDatabase

    private lateinit var loadingDialog: LoadingDialog
    private lateinit var timePickerDialog: TimePickerFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityEditSwitchBinding.inflate(layoutInflater)
        setContentView(bind.root)

        myDB = MyFirebaseDatabase()
        iconsList = resources.obtainTypedArray(R.array.icons_list)
        iconsNameList = resources.getStringArray(R.array.icons_names)

        loadingDialog = LoadingDialog()
        timePickerDialog = TimePickerFragment()

        roomId = intent.getStringExtra(ROOM_ID)!!
        switchId = intent.getStringExtra(SWITCH_ID)!!

        bind.esBackBtn.setOnClickListener {
            startActivity(Intent(this, StartActivity::class.java))
            finish()
        }

        bind.switchIcon.setOnClickListener { showChooseIconDialog() }
        bind.startTimePicker.setOnClickListener {
            TIME_PICKER_FLAG = 1
            setStartTime() }
        bind.stopTimePicker.setOnClickListener {
            TIME_PICKER_FLAG = 2
            setStopTime() }

        bind.esSubmitBtn.setOnClickListener { checkData() }
    }

    private fun setStartTime() {
        TIME_PICKER_FLAG = 1
        timePickerDialog.show(supportFragmentManager, TAG)
    }

    private fun setStopTime() {
        TIME_PICKER_FLAG = 2
        timePickerDialog.show(supportFragmentManager, TAG)
    }

    private fun checkData() {
        loadingDialog.show(supportFragmentManager, TAG)
        val switchName = bind.switchNameEt.text.toString()

        if (switchName.trim().isNotBlank()) {
            if (START_TIME_FLAG or START_TIME_FLAG) {
                if (bind.sunCb.isChecked || bind.monCb.isChecked || bind.monCb.isChecked || bind.monCb.isChecked
                    || bind.monCb.isChecked || bind.monCb.isChecked || bind.monCb.isChecked) {
                    submitData(switchName)
                } else {
                    loadingDialog.dismiss()
                    Snackbar.make(bind.esRootView, "Please select day(s) to set timing.",
                        Snackbar.LENGTH_SHORT).show()
                }
            } else submitData(switchName)
        } else {
            loadingDialog.dismiss()
            bind.switchNameEt.error = "Name is required"
        }
    }

    private fun submitData(switchName: String) {
        myDB.dbProfileRef.child("devices").child(roomId).child(switchId).apply {
            child("name").setValue(switchName)
            child("icon").setValue(switchIconIndex)
            child(START_TIME).setValue(bind.startTimeEt.text.toString())
            child(STOP_TIME).setValue(bind.stopTimeEt.text.toString())
            child(SUN).setValue(if (bind.sunCb.isChecked) ONE else ZERO)
            child(MON).setValue(if (bind.monCb.isChecked) ONE else ZERO)
            child(TUE).setValue(if (bind.tueCb.isChecked) ONE else ZERO)
            child(WED).setValue(if (bind.wedCb.isChecked) ONE else ZERO)
            child(THU).setValue(if (bind.thuCb.isChecked) ONE else ZERO)
            child(FRI).setValue(if (bind.friCb.isChecked) ONE else ZERO)
            child(SAT).setValue(if (bind.satCb.isChecked) ONE else ZERO)
                .addOnSuccessListener {

                    setTimer()

                    loadingDialog.dismiss()
                goToStartActivity()
            }
        }
    }

    private fun setTimer() {
        val switch = when (switchId) {
            StartActivity.SWITCH1 -> 1
            StartActivity.SWITCH2 -> 2
            StartActivity.SWITCH3 -> 3
            else -> 4
        }
        START_REQUEST_CODE += (roomId.toInt() * 100) + switch
        STOP_REQUEST_CODE += (roomId.toInt() * 100) + switch

        Log.d(TAG, "setTimer: $START_REQUEST_CODE | $STOP_REQUEST_CODE")
        Toast.makeText(this, "$START_REQUEST_CODE | $STOP_REQUEST_CODE", Toast.LENGTH_SHORT).show()

        if (START_TIME_FLAG) {
            Log.d(TAG, "setTimer: START_TIME Called")
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, AlertReceiver::class.java)
            intent.putExtra(ROOM_ID, roomId)
            intent.putExtra(SWITCH_ID, switchId)
            intent.action = START
            val pendingIntent = PendingIntent.getBroadcast(this, START_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)

//            if (cal1.before(Calendar.getInstance())) cal1.add(Calendar.DATE, 1)

            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal1.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)
        }
        if (STOP_TIME_FLAG) {
            Log.d(TAG, "setTimer: STOP_TIME Called")
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, AlertReceiver::class.java)
            intent.putExtra(ROOM_ID, roomId)
            intent.putExtra(SWITCH_ID, switchId)
            intent.action = STOP
            val pendingIntent = PendingIntent.getBroadcast(this, STOP_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)

//            if (cal2.before(Calendar.getInstance())) cal2.add(Calendar.DATE, 1)

            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal2.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)
        }
    }

    private fun showChooseIconDialog() {
        val builder = AlertDialog.Builder(this)
        val listDialogInflater = layoutInflater.inflate(R.layout.icons_list_dialog_layout, null)

        val iconListAdapter = IconListAdapter(this, iconsNameList, iconsList)
        val listView = listDialogInflater.findViewById<ListView>(R.id.icon_listview)
        listView.adapter = iconListAdapter

        builder.setView(listDialogInflater)
        builder.setPositiveButton("Cancel") { _, _ -> }
        val dialog = builder.create()
        dialog.show()

        listView.setOnItemClickListener { _, _, position, _ ->
            bind.switchIcon.setImageResource(iconsList.getResourceId(position, 0))
            switchIconIndex = position
//            myDbHandler.dbProfileRef.child("devices").child(roomId!!).child("switch").child("icon").setValue(position)
            dialog.dismiss()
        }
    }

    private fun loadUIData() {
        if (StartActivity().isOnline(this)) {
            myDB.dbProfileRef.child(DEVICES).child(roomId).get().addOnSuccessListener {
                Log.i(TAG, "loadUIData: $it")
                bind.esRoomNameTv.text = it.child("name").value.toString()
                bind.esRoomIdTv.text = roomId
                bind.switchNameTv.text = it.child(switchId).child("name").value.toString()
                switchIconIndex = (it.child(switchId).child("icon").value as Long).toInt()
                bind.switchIcon.setImageResource(iconsList.getResourceId(switchIconIndex, 0))
                bind.switchNameEt.setText(it.child(switchId).child("name").value.toString())

                bind.startTimeEt.setText(it.child(switchId).child(START_TIME).value.toString())
                bind.stopTimeEt.setText(it.child(switchId).child(STOP_TIME).value.toString())

                bind.sunCb.isChecked = it.child(switchId).child(SUN).value.toString() == "1"
                bind.monCb.isChecked = it.child(switchId).child(MON).value.toString() == "1"
                bind.tueCb.isChecked = it.child(switchId).child(TUE).value.toString() == "1"
                bind.wedCb.isChecked = it.child(switchId).child(WED).value.toString() == "1"
                bind.thuCb.isChecked = it.child(switchId).child(THU).value.toString() == "1"
                bind.friCb.isChecked = it.child(switchId).child(FRI).value.toString() == "1"
                bind.satCb.isChecked = it.child(switchId).child(SAT).value.toString() == "1"
            }
        } else {
            Snackbar.make(bind.esRootView, "No internet.", Snackbar.LENGTH_INDEFINITE)
                .setAction("Retry") {
                    loadUIData()
                }
                .show()
        }
    }

    private fun goToStartActivity() {
        val intent = Intent(this, StartActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()

        loadUIData()
    }

    companion object {
        const val ROOM_ID = "room_id"
        const val SWITCH_ID = "switch_id"
        const val START = "START"
        const val STOP = "STOP"
    }

    override fun onTimeSet(p0: TimePicker?, hour: Int, minute: Int) {

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)

//        val timeUnit = if (hour < 12) "AM" else "PM"
//        val time = "${if (hour > 12) hour-12 else hour}:$minute $timeUnit"
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(cal.time)

        if (TIME_PICKER_FLAG == 1) {
            cal1 = cal
            bind.startTimeEt.setText(time)
            START_TIME_FLAG = true
        }
        if (TIME_PICKER_FLAG == 2) {
            cal2 = cal
            bind.stopTimeEt.setText(time)
            STOP_TIME_FLAG = true
        }
    }
}