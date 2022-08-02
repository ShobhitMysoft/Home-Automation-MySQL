package com.mysofttechnology.homeautomation.activities

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.res.TypedArray
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.toolbox.StringRequest
import com.google.android.material.snackbar.Snackbar
import com.mysofttechnology.homeautomation.LoadingDialog
import com.mysofttechnology.homeautomation.R
import com.mysofttechnology.homeautomation.StartActivity
import com.mysofttechnology.homeautomation.StartActivity.Companion.BLANK
import com.mysofttechnology.homeautomation.StartActivity.Companion.FRI
import com.mysofttechnology.homeautomation.StartActivity.Companion.MON
import com.mysofttechnology.homeautomation.StartActivity.Companion.ONE
import com.mysofttechnology.homeautomation.StartActivity.Companion.SAT
import com.mysofttechnology.homeautomation.StartActivity.Companion.START_TIME
import com.mysofttechnology.homeautomation.StartActivity.Companion.STOP_TIME
import com.mysofttechnology.homeautomation.StartActivity.Companion.SUN
import com.mysofttechnology.homeautomation.StartActivity.Companion.SWITCH
import com.mysofttechnology.homeautomation.StartActivity.Companion.THU
import com.mysofttechnology.homeautomation.StartActivity.Companion.TUE
import com.mysofttechnology.homeautomation.StartActivity.Companion.WED
import com.mysofttechnology.homeautomation.StartActivity.Companion.ZERO
import com.mysofttechnology.homeautomation.adapters.IconListAdapter
import com.mysofttechnology.homeautomation.databinding.ActivityEditSwitchBinding
import com.mysofttechnology.homeautomation.utils.TimePickerFragment
import com.mysofttechnology.homeautomation.utils.VolleySingleton
import org.json.JSONArray
import org.json.JSONObject
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
    private lateinit var deviceId: String
    private lateinit var userId: String
    private lateinit var roomName: String
    private lateinit var switchId: String
    private lateinit var switchIdByApp: String

    private var switchIconIndex: Int = 0
    private lateinit var iconsList: TypedArray
    private lateinit var iconsNameList: Array<String>

    private lateinit var loadingDialog: LoadingDialog
    private lateinit var timePickerDialog: TimePickerFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityEditSwitchBinding.inflate(layoutInflater)
        setContentView(bind.root)

        loadingDialog = LoadingDialog()
        timePickerDialog = TimePickerFragment()

        deviceId = intent.getStringExtra(ROOM_ID)!!
        userId = intent.getStringExtra(USER_ID)!!
        roomName = intent.getStringExtra(ROOM_NAME)!!
        switchId = intent.getStringExtra(SWITCH_ID)!!
        switchIdByApp = intent.getStringExtra(SWITCH_ID_BY_APP)!!

        iconsList = if (switchIdByApp == "6") resources.obtainTypedArray(R.array.sl1_icons_list) else resources.obtainTypedArray(R.array.icons_list)
        iconsNameList = if (switchIdByApp == "6") resources.getStringArray(R.array.sl1_icons_names) else resources.getStringArray(R.array.icons_names)

        loadUIData()

        bind.esBackBtn.setOnClickListener {
            startActivity(Intent(this, StartActivity::class.java))
            finish()
        }

        bind.switchIcon.setOnClickListener { showChooseIconDialog() }
        bind.startTimePicker.setOnClickListener {
            TIME_PICKER_FLAG = 1
            setStartTime()
        }
        bind.stopTimePicker.setOnClickListener {
            TIME_PICKER_FLAG = 2
            setStopTime()
        }

        bind.esSubmitBtn.setOnClickListener { checkData() }
        bind.esRemoveTimingBtn.setOnClickListener { clearTiming() }
    }

    private fun clearTiming() {
        bind.startTimeTv.text = BLANK
        bind.stopTimeTv.text = BLANK

        bind.sunCb.isChecked = false
        bind.monCb.isChecked = false
        bind.tueCb.isChecked = false
        bind.wedCb.isChecked = false
        bind.thuCb.isChecked = false
        bind.friCb.isChecked = false
        bind.satCb.isChecked = false
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
            if (bind.startTimeTv.text.isNotBlank() or bind.stopTimeTv.text.isNotBlank()) {
                if (bind.sunCb.isChecked || bind.monCb.isChecked || bind.tueCb.isChecked || bind.wedCb.isChecked
                    || bind.thuCb.isChecked || bind.friCb.isChecked || bind.satCb.isChecked
                ) {
                    submitData(switchName)
                } else {
                    loadingDialog.dismiss()
                    Snackbar.make(bind.esRootView, "Please select week day(s).",
                        Snackbar.LENGTH_LONG)
                        .setAction("Cancel") {
                            clearTiming()
                        }
                        .show()
                }
            } else submitData(switchName)
        } else {
            loadingDialog.dismiss()
            bind.switchNameEt.error = "Name is required"
        }
    }

    private fun submitData(switchName: String) {

        val requestQueue = VolleySingleton.getInstance(this).requestQueue
        val url = getString(R.string.base_url) + getString(R.string.url_switch)

        val stringRequest = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val mData = JSONObject(response.toString())
                    val resp = mData.get("response") as Int
                    val msg = mData.get("msg")

                    if (resp == 1) {
                        goToStartActivity()
                        Log.d(TAG, "createSwitch: Message - $msg")
                    } else {
                        loadingDialog.dismiss()
                        showToast("unable to create room")
                        Log.e(TAG, "createSwitch: Message - $msg")
                    }
                } catch (e: Exception) {
                    loadingDialog.dismiss()
                    Log.e(TAG, "Exception in submitData: $e")
                    if (e.message != null) showToast(e.message)
                }
            }, {
                loadingDialog.dismiss()
                showToast("Something went wrong.")
                Log.e(TAG, "VollyError: ${it.message}")
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["device_id"] = deviceId
                params["switch_id"] = switchId
                params["mobile_no"] = userId
                params["switch"] = switchName
                params["icon"] = switchIconIndex.toString()
                params[START_TIME] = bind.startTimeTv.text.toString().trim().replace(" ", ":")
                params[STOP_TIME] = bind.stopTimeTv.text.toString().trim().replace(" ", ":")
                params[SUN] = if (bind.sunCb.isChecked) ONE else ZERO
                params[MON] = if (bind.monCb.isChecked) ONE else ZERO
                params[TUE] = if (bind.tueCb.isChecked) ONE else ZERO
                params[WED] = if (bind.wedCb.isChecked) ONE else ZERO
                params[THU] = if (bind.thuCb.isChecked) ONE else ZERO
                params[FRI] = if (bind.friCb.isChecked) ONE else ZERO
                params[SAT] = if (bind.satCb.isChecked) ONE else ZERO
                params["switch_id_by_app"] = switchIdByApp

                return params
            }

            override fun getHeaders(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/x-www-form-urlencoded"
                return params
            }
        }
        requestQueue.add(stringRequest)
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
            dialog.dismiss()
        }
    }

    private fun loadUIData() {
        loadingDialog.show(supportFragmentManager, TAG)
        val requestQueue = VolleySingleton.getInstance(this).requestQueue
        val switchListUrl = getString(R.string.base_url) + getString(R.string.url_switch_list)

        if (StartActivity().isOnline(this)) {

            val switchListRequest = object : StringRequest(Method.POST, switchListUrl,
                { response ->
                    Log.i(TAG, "updateUI: $response")
                    try {
                        val mData = JSONObject(response.toString())
                        val resp = mData.get("response") as Int
                        val msg = mData.get("msg")

                        if (resp == 1) {
                            val switchListData = mData.get("data") as JSONArray
                            for (i in 0..switchListData.length()) {
                                val switchData = switchListData.get(i) as JSONObject
                                if (switchData.get("id") == switchId) {
                                    updateUI(switchData)
                                    break
                                }
                            }

                            Log.d(TAG, "updateUI: Message - $msg")
                        } else {
                            loadingDialog.dismiss()
                            showToast("Failed to get data")
//                        showErrorScreen()
                            Log.e(TAG, "switch updateUI: Message - $msg")
                        }
                    } catch (e: Exception) {
                        loadingDialog.dismiss()
                        Log.e(TAG, "Exception in switch updateUI: $e")
                        if (e.message != null) showToast(e.message)
                    }
                }, {
                    loadingDialog.dismiss()
                    showToast("Something went wrong.")
                    Log.e(TAG, "VollyError: ${it.message}")
                }) {
                override fun getParams(): Map<String, String> {
                    val params = HashMap<String, String>()
                    params["device_id"] = deviceId
                    params["mobile_no"] = userId
                    return params
                }

                override fun getHeaders(): MutableMap<String, String> {
                    val params = HashMap<String, String>()
                    params["Content-Type"] = "application/x-www-form-urlencoded"
                    return params
                }
            }
            requestQueue.add(switchListRequest)
        } else {
            Snackbar.make(bind.esRootView, "No internet.", Snackbar.LENGTH_INDEFINITE)
                .setAction("Retry") {
                    loadUIData()
                }
                .show()
        }
    }

    private fun updateUI(switchData: JSONObject) {
        bind.esRoomNameTv.text = roomName
        bind.esRoomIdTv.text = deviceId
        bind.switchNameTv.text = switchData.get(SWITCH).toString()
        switchIconIndex = (switchData.get("icon") as String).toInt()
        bind.switchIcon.setImageResource(iconsList.getResourceId(switchIconIndex, 0))
        bind.switchNameEt.setText(switchData.get(SWITCH).toString())

        bind.startTimeTv.text = switchData.get(START_TIME).toString()
        bind.stopTimeTv.text = switchData.get(STOP_TIME).toString()

        bind.sunCb.isChecked = switchData.get(SUN).toString() == "1"
        bind.monCb.isChecked = switchData.get(MON).toString() == "1"
        bind.tueCb.isChecked = switchData.get(TUE).toString() == "1"
        bind.wedCb.isChecked = switchData.get(WED).toString() == "1"
        bind.thuCb.isChecked = switchData.get(THU).toString() == "1"
        bind.friCb.isChecked = switchData.get(FRI).toString() == "1"
        bind.satCb.isChecked = switchData.get(SAT).toString() == "1"

        loadingDialog.dismiss()
    }

    private fun goToStartActivity() {
        val intent = Intent(this, StartActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String?) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        const val ROOM_ID = "device_id"
        const val USER_ID = "user_id"
        const val ROOM_NAME = "room_name"
        const val SWITCH_ID = "switch_id"
        const val SWITCH_ID_BY_APP = "switch_id_by_app"
    }

    override fun onTimeSet(p0: TimePicker?, hour: Int, minute: Int) {

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)

        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(cal.time)

        if (TIME_PICKER_FLAG == 1) {
            cal1 = cal
            bind.startTimeTv.text = time
            START_TIME_FLAG = true
        }
        if (TIME_PICKER_FLAG == 2) {
            cal2 = cal
            bind.stopTimeTv.text = time
            STOP_TIME_FLAG = true
        }
    }
}