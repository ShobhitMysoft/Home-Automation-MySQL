package com.mysofttechnology.homeautomation

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.TypedArray
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.mysofttechnology.homeautomation.StartActivity.Companion.APPL1
import com.mysofttechnology.homeautomation.StartActivity.Companion.APPL2
import com.mysofttechnology.homeautomation.StartActivity.Companion.APPL3
import com.mysofttechnology.homeautomation.StartActivity.Companion.APPL4
import com.mysofttechnology.homeautomation.StartActivity.Companion.FAN
import com.mysofttechnology.homeautomation.StartActivity.Companion.FRI
import com.mysofttechnology.homeautomation.StartActivity.Companion.ICON
import com.mysofttechnology.homeautomation.StartActivity.Companion.MON
import com.mysofttechnology.homeautomation.StartActivity.Companion.NOTIFICATION
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
import com.mysofttechnology.homeautomation.activities.EditSwitchActivity
import com.mysofttechnology.homeautomation.activities.EditSwitchActivity.Companion.ROOM_ID
import com.mysofttechnology.homeautomation.activities.EditSwitchActivity.Companion.ROOM_NAME
import com.mysofttechnology.homeautomation.activities.EditSwitchActivity.Companion.SWITCH_ID
import com.mysofttechnology.homeautomation.activities.EditSwitchActivity.Companion.SWITCH_ID_BY_APP
import com.mysofttechnology.homeautomation.activities.ErrorActivity
import com.mysofttechnology.homeautomation.databinding.FragmentRoomControlsBinding
import com.mysofttechnology.homeautomation.utils.VolleySingleton
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "RoomControlsFragment"

class RoomControlsFragment : Fragment() {

    private var liveFanSpeed: Int = 0
    private var SWITCH1: String? = null
    private var SWITCH2: String? = null
    private var SWITCH3: String? = null
    private var SWITCH4: String? = null
    private val CHECK_WIFI_DELAY_TIME: Long = 4000
    private lateinit var toggleWifi: Handler
    private var checkWifiIsRunning: Boolean = false
    private var checkWifi: Boolean = false
    private var sharedPref: SharedPreferences? = null
    private lateinit var requestQueue: RequestQueue
    private lateinit var waitSnackbar: Snackbar

    private var currentDeviceId: String? = null
    private var currentUserId: String? = null

    private var _binding: FragmentRoomControlsBinding? = null
    private val binding get() = _binding!!

    private lateinit var loadingDialog: LoadingDialog

    private var roomsList: ArrayList<String> = arrayListOf()
    private var deviceIDList: ArrayList<String> = arrayListOf()

    private lateinit var iconsList: TypedArray
    private var selectedRoomIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate: Called")

        val exitAppDialog = ExitAppDialog()
        loadingDialog = LoadingDialog()
        loadingDialog.isCancelable = false

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            exitAppDialog.show(childFragmentManager, "Exit App")
        }

        callback.isEnabled = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        _binding = FragmentRoomControlsBinding.inflate(inflater, container, false)

        iconsList = resources.obtainTypedArray(R.array.icons_list)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated: Called")

        requestQueue = VolleySingleton.getInstance(requireContext()).requestQueue

        sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE) ?: return
        currentUserId = sharedPref!!.getString(getString(R.string.current_user_id), "")
        Log.d(TAG, "onViewCreated: $currentUserId")

        toggleWifi = Handler()
        waitSnackbar =
            Snackbar.make(requireActivity().findViewById(android.R.id.content), "Please wait...",
                Snackbar.LENGTH_INDEFINITE)

        Log.d(TAG, "onViewCreated: currentDeviceId - $currentDeviceId")

        binding.currentRoomTv.setOnClickListener {
            showChooseRoomDialog()
        }

        binding.switch1Card.setOnLongClickListener {
            Toast.makeText(requireContext(), "Long press detected", Toast.LENGTH_SHORT).show()
            true
        }

        uiHandler()
    }

    private fun checkDatabase() {
        loadingDialog.show(childFragmentManager, "$TAG checkDatabase")
        val requestQueue = VolleySingleton.getInstance(requireContext()).requestQueue
        val url = getString(R.string.base_url) + getString(R.string.url_room_list)

        if (isOnline()) {
            roomsList.clear()
            deviceIDList.clear()

            val stringRequest = object : StringRequest(Method.POST, url,
                { response ->
                    try {
                        val mData = JSONObject(response.toString())
                        val resp = mData.get("response") as Int
                        val msg = mData.get("msg")

                        if (resp == 1) {
                            val roomListData = mData.get("data") as JSONArray
                            createRoom(roomListData)

                            Log.d(TAG, "checkDatabase: Message - $msg")
                        } else {
                            loadingDialog.dismiss()
                            Log.d(TAG, "checkDatabase: Message - $msg")

                            gotoAddDevice()
                        }
                    } catch (e: Exception) {
                        loadingDialog.dismiss()
                        Log.e(TAG, "Exception in checkDatabase: $e")
                        showLToast(e.message)
                    }
                }, {
                    loadingDialog.dismiss()
                    showLToast("Something went wrong.")
                    Log.e(TAG, "VollyError: ${it.message}")
                }) {
                override fun getParams(): Map<String, String> {
                    val params = HashMap<String, String>()
                    params["user_id"] = currentUserId.toString()
                    return params
                }

                override fun getHeaders(): MutableMap<String, String> {
                    val params = HashMap<String, String>()
                    params["Content-Type"] = "application/x-www-form-urlencoded"
                    return params
                }
            }
            requestQueue.add(stringRequest)
        } else {
            loadingDialog.dismiss()
            showPSnackbar("No internet connection")
        }
    }

    private fun createRoom(roomListData: JSONArray) {
        for (i in 0 until roomListData.length()) {
            val device = roomListData.getJSONObject(i)
            roomsList.add(device.get("room_name").toString())
            deviceIDList.add(device.get("device_id").toString())
        }

        currentDeviceId = deviceIDList[selectedRoomIndex]
        binding.currentRoomTv.text = roomsList[selectedRoomIndex]
        updateUI()
    }

    private fun showErrorScreen() {
        if (isAdded) {
            val intent = Intent(context, ErrorActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            context?.startActivity(intent)
        } else Log.e(TAG, "showErrorScreen: RoomControlsFragment is not attached to an activity.")
    }

    private fun updateUI() {
        Log.i(TAG, "updateUI: Called $currentDeviceId")
        val getLiveUrl = getString(R.string.base_url) + getString(R.string.url_get_live)
        val switchListUrl = getString(R.string.base_url) + getString(R.string.url_switch_list)

        val liveDataRequest = object : StringRequest(Method.POST, getLiveUrl,
            { response ->
                Log.i(TAG, "updateUI: $response")
                try {
                    val mData = JSONObject(response.toString())
                    val resp = mData.get("response") as Int
                    val msg = mData.get("msg")

                    if (resp == 1) {
                        val app1Val = mData.get(APPL1).toString()
                        val app2Val = mData.get(APPL2).toString()
                        val app3Val = mData.get(APPL3).toString()
                        val app4Val = mData.get(APPL4).toString()
                        val fan = mData.get(FAN).toString()

                        val wifi = mData.get("wifi").toString()

                        if (checkWifi && wifi == "0") showDeviceOfflineDialog()
                        else {
                            updateLive("0", "wifi")
                            liveFanSpeed = fan.toInt()

                            binding.switch1Switch.isChecked = app1Val == ONE
                            binding.switch2Switch.isChecked = app2Val == ONE
                            binding.switch3Switch.isChecked = app3Val == ONE
                            binding.switch4Switch.isChecked = app4Val == ONE

                            if (liveFanSpeed == 0) {
                                binding.fanSpeedSlider.value = 0.0f
                                binding.fanSpeedTv.text = ZERO
                                if (binding.fanSwitch.isChecked) binding.fanSwitch.isChecked = false
                            } else {
                                binding.fanSpeedSlider.value = liveFanSpeed.toFloat()
                                binding.fanSpeedTv.text = fan
                                if (!binding.fanSwitch.isChecked) binding.fanSwitch.isChecked = true
                            }

                            checkWifi = false
                            togglePower(app1Val, app2Val, app3Val, app4Val, fan)
                        }

                        Log.d(TAG, "updateUI: Message - $msg")
                    } else {
                        loadingDialog.dismiss()
                        showPSnackbar("Failed to get room data")
                        Log.e(TAG, "updateUI: Message - $msg")
                    }
                } catch (e: Exception) {
                    loadingDialog.dismiss()
                    Log.e(TAG, "Exception in updateUI: $e")
                    showLToast(e.message)
                }
            }, {
                loadingDialog.dismiss()
                showLToast("Something went wrong.")
                Log.e(TAG, "VollyError: ${it.message}")
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["device_id"] = currentDeviceId.toString()
                return params
            }

            override fun getHeaders(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/x-www-form-urlencoded"
                return params
            }
        }

        val switchListRequest = object : StringRequest(Method.POST, switchListUrl,
            { response ->
                Log.i(TAG, "switchList: $response")
                try {
                    val mData = JSONObject(response.toString())
                    val resp = mData.get("response") as Int
                    val msg = mData.get("msg")

                    if (resp == 1) {
                        val switchListData = mData.get("data") as JSONArray
                        for (i in 0..4) {
                            val switchData = switchListData.getJSONObject(i)
                            if (switchData.get("switch_id_by_app").toString() != "5")
                                loadSwitch(switchData.get("switch_id_by_app").toString(), switchData)
                        }
                        Log.d(TAG, "switchList: Message - $msg")
                    } else {
                        loadingDialog.dismiss()
                        showPSnackbar("Failed to get room data")
                        Log.e(TAG, "switch switchList: Message - $msg")
                    }
                } catch (e: Exception) {
                    loadingDialog.dismiss()
                    Log.e(TAG, "Exception in switch updateUI: $e")
                    showLToast(e.message)
                }
            }, {
                loadingDialog.dismiss()
                showLToast("Something went wrong.")
                Log.e(TAG, "VollyError: ${it.message}")
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["device_id"] = currentDeviceId.toString()
                return params
            }

            override fun getHeaders(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/x-www-form-urlencoded"
                return params
            }
        }

        requestQueue.add(liveDataRequest)
        requestQueue.add(switchListRequest)
    }

    private fun showDeviceOfflineDialog() {
        checkWifi = false
        loadingDialog.dismiss()
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Device Offline")
            .setMessage(
                "${roomsList[selectedRoomIndex]} is currently offline. Please make sure the device is connected to wifi.")
            .setPositiveButton("Ok"
            ) { _, _ -> enableUI() }
        builder.create()
        builder.show()
    }

    private fun loadSwitch(switchId: String, switch: JSONObject) {
        when (switchId) {
            "1" -> SWITCH1 = switch.get("id").toString()
            "2" -> SWITCH2 = switch.get("id").toString()
            "3" -> SWITCH3 = switch.get("id").toString()
            else -> SWITCH4 = switch.get("id").toString()
        }
        val switchName = when (switchId) {
            "1" -> binding.switch1Name
            "2" -> binding.switch2Name
            "3" -> binding.switch3Name
            else -> binding.switch4Name
        }
        val switchIcon = when (switchId) {
            "1" -> binding.switch1Icon
            "2" -> binding.switch2Icon
            "3" -> binding.switch3Icon
            else -> binding.switch4Icon
        }
        val switchStartTime = when (switchId) {
            "1" -> binding.switch1StartTimeTv
            "2" -> binding.switch2StartTimeTv
            "3" -> binding.switch3StartTimeTv
            else -> binding.switch4StartTimeTv
        }
        val switchStopTime = when (switchId) {
            "1" -> binding.switch1StopTimeTv
            "2" -> binding.switch2StopTimeTv
            "3" -> binding.switch3StopTimeTv
            else -> binding.switch4StopTimeTv
        }

        val sunTv = when (switchId) {
            "1" -> binding.switch1SunTv
            "2" -> binding.switch2SunTv
            "3" -> binding.switch3SunTv
            else -> binding.switch4SunTv
        }
        val monTv = when (switchId) {
            "1" -> binding.switch1MonTv
            "2" -> binding.switch2MonTv
            "3" -> binding.switch3MonTv
            else -> binding.switch4MonTv
        }
        val tueTv = when (switchId) {
            "1" -> binding.switch1TueTv
            "2" -> binding.switch2TueTv
            "3" -> binding.switch3TueTv
            else -> binding.switch4TueTv
        }
        val wedTv = when (switchId) {
            "1" -> binding.switch1WedTv
            "2" -> binding.switch2WedTv
            "3" -> binding.switch3WedTv
            else -> binding.switch4WedTv
        }
        val thuTv = when (switchId) {
            "1" -> binding.switch1ThuTv
            "2" -> binding.switch2ThuTv
            "3" -> binding.switch3ThuTv
            else -> binding.switch4ThuTv
        }
        val friTv = when (switchId) {
            "1" -> binding.switch1FriTv
            "2" -> binding.switch2FriTv
            "3" -> binding.switch3FriTv
            else -> binding.switch4FriTv
        }
        val satTv = when (switchId) {
            "1" -> binding.switch1SatTv
            "2" -> binding.switch2SatTv
            "3" -> binding.switch3SatTv
            else -> binding.switch4SatTv
        }

        switchName.text = switch.getString(SWITCH)
        switchIcon.setImageResource(
            iconsList.getResourceId(switch.getString(ICON).toInt(), 0))

        var startTime = switch.getString(START_TIME).trim()
        if (startTime.length > 4) {
            startTime = startTime.substring(0, 4) + " " + startTime.substring(5, startTime.length)
        }
        switchStartTime.text = startTime

        var stopTime = switch.getString(STOP_TIME).trim()
        if (stopTime.length > 4) {
            stopTime = stopTime.substring(0, 4) + " " + stopTime.substring(5, stopTime.length)
        }
        switchStopTime.text = stopTime

        if (switch.getString(SUN) == ONE) {
            sunTv.setTextColor(ContextCompat.getColor(context!!, R.color.colorAccent))
            sunTv.typeface = Typeface.DEFAULT_BOLD
        }
        if (switch.getString(MON) == ONE) {
            monTv.setTextColor(ContextCompat.getColor(context!!, R.color.colorAccent))
            monTv.typeface = Typeface.DEFAULT_BOLD
        }
        if (switch.getString(TUE) == ONE) {
            tueTv.setTextColor(ContextCompat.getColor(context!!, R.color.colorAccent))
            tueTv.typeface = Typeface.DEFAULT_BOLD
        }
        if (switch.getString(WED) == ONE) {
            wedTv.setTextColor(ContextCompat.getColor(context!!, R.color.colorAccent))
            wedTv.typeface = Typeface.DEFAULT_BOLD
        }
        if (switch.getString(THU) == ONE) {
            thuTv.setTextColor(ContextCompat.getColor(context!!, R.color.colorAccent))
            thuTv.typeface = Typeface.DEFAULT_BOLD
        }
        if (switch.getString(FRI) == ONE) {
            friTv.setTextColor(ContextCompat.getColor(context!!, R.color.colorAccent))
            friTv.typeface = Typeface.DEFAULT_BOLD
        }
        if (switch.getString(SAT) == ONE) {
            satTv.setTextColor(ContextCompat.getColor(context!!, R.color.colorAccent))
            satTv.typeface = Typeface.DEFAULT_BOLD
        }

        checkForNotification(switch.get("id").toString(), switch.getString(SWITCH), switch.getString(NOTIFICATION))
    }

    private fun checkForNotification(switchId: String, switchName: String, notif: String) {
        when (notif) {
            "1" -> {
                showSToast("$switchName was turned ON on time.")
                updateSwitch(switchId)
            }
            "2" -> {
                showSToast("$switchName was turned OFF on time.")
                updateSwitch(switchId)
            }
        }
    }

    private fun updateSwitch(switchId: String) {
        val requestQueue = VolleySingleton.getInstance(requireContext()).requestQueue
        val url = getString(R.string.base_url) + getString(R.string.url_switch_notification)

        val stringRequest = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val mData = JSONObject(response.toString())
                    val resp = mData.get("response") as Int
                    val msg = mData.get("msg")

                    if (resp == 1) {
                        Log.d(TAG, "updateSwitch: Message - $msg")
                    } else {
                        loadingDialog.dismiss()
                        showLToast("unable to create room")
                        Log.e(TAG, "updateSwitch: Message - $msg")
                    }
                } catch (e: Exception) {
                    loadingDialog.dismiss()
                    Log.e(TAG, "Exception in updateSwitch: $e")
                }
            }, {
                loadingDialog.dismiss()
                Log.e(TAG, "VollyError: ${it.message}")
            }) {
            override fun getParams(): Map<String, String> {
                val params = java.util.HashMap<String, String>()
                params["switch_id"] = switchId
                params["notification"] = "0"

                return params
            }

            override fun getHeaders(): MutableMap<String, String> {
                val params = java.util.HashMap<String, String>()
                params["Content-Type"] = "application/x-www-form-urlencoded"
                return params
            }
        }
        requestQueue.add(stringRequest)
    }

    private fun uiHandler() {
        Log.d(TAG, "uiHandler: Called\n")
        val spEditor = sharedPref?.edit()

        binding.powerBtn.setOnClickListener {
            loadingDialog.show(childFragmentManager, "$TAG powerBtn")
            if (!checkWifiIsRunning) {
                checkWifiIsRunning = true
                toggleWifi.postDelayed(wifiRunnable, CHECK_WIFI_DELAY_TIME)
            }
            togglePower()
        }

        binding.fanSpeedSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}

            override fun onStopTrackingTouch(slider: Slider) {
                disableUI()
                if (!checkWifiIsRunning) {
                    checkWifiIsRunning = true
                    toggleWifi.postDelayed(wifiRunnable, CHECK_WIFI_DELAY_TIME)
                }
                val speed = slider.value

                updateLive(speed.toInt().toString(), FAN)

                spEditor?.putString("old_fan_speed_$currentDeviceId)", speed.toInt().toString())
                spEditor?.apply()
            }
        })

        binding.fanSwitch.setOnCheckedChangeListener { _, isChecked ->
            disableUI()
            if (!checkWifiIsRunning) {
                checkWifiIsRunning = true
                toggleWifi.postDelayed(wifiRunnable, CHECK_WIFI_DELAY_TIME)
            }
            if (!isChecked) {
                updateLive(ZERO, FAN)
            } else {
                val oldFanSpeed = sharedPref!!.getString("old_fan_speed_$currentDeviceId)", liveFanSpeed.toString())
                updateLive(oldFanSpeed.toString(), FAN)
            }
        }

        binding.switch1Switch.setOnCheckedChangeListener { _, isChecked ->
            disableUI()
            if (!checkWifiIsRunning) {
                checkWifiIsRunning = true
                toggleWifi.postDelayed(wifiRunnable, CHECK_WIFI_DELAY_TIME)
            }
            updateLive(if (isChecked) ONE else ZERO, APPL1)
        }

        binding.switch2Switch.setOnCheckedChangeListener { _, isChecked ->
            disableUI()
            if (!checkWifiIsRunning) {
                checkWifiIsRunning = true
                toggleWifi.postDelayed(wifiRunnable, CHECK_WIFI_DELAY_TIME)
            }
            updateLive(if (isChecked) ONE else ZERO, APPL2)
        }

        binding.switch3Switch.setOnCheckedChangeListener { _, isChecked ->
            disableUI()
            if (!checkWifiIsRunning) {
                checkWifiIsRunning = true
                toggleWifi.postDelayed(wifiRunnable, CHECK_WIFI_DELAY_TIME)
            }
            updateLive(if (isChecked) ONE else ZERO, APPL3)
        }

        binding.switch4Switch.setOnCheckedChangeListener { _, isChecked ->
            disableUI()
            if (!checkWifiIsRunning) {
                checkWifiIsRunning = true
                toggleWifi.postDelayed(wifiRunnable, CHECK_WIFI_DELAY_TIME)
            }
            updateLive(if (isChecked) ONE else ZERO, APPL4)
        }

        binding.switch1MoreBtn.setOnClickListener {
            SWITCH1?.let { id -> showPopupMenu(it, id, "1") }
        }
        binding.switch2MoreBtn.setOnClickListener {
            SWITCH2?.let { id -> showPopupMenu(it, id, "2") }
        }
        binding.switch3MoreBtn.setOnClickListener {
            SWITCH3?.let { id -> showPopupMenu(it, id, "3") }
        }
        binding.switch4MoreBtn.setOnClickListener {
            SWITCH4?.let { id -> showPopupMenu(it, id, "4") }
        }
    }

    private fun updateLive(value: String, appl: String) {
        val liveUpdateUrl = getString(R.string.base_url) + getString(R.string.url_live_update)

        val liveUpdateRequest = object : StringRequest(Method.POST, liveUpdateUrl,
            { response ->
                Log.i(TAG, "updateUI: $response")
                try {
                    val mData = JSONObject(response.toString())
                    val resp = mData.get("response") as Int
                    val msg = mData.get("msg")

                    if (resp == 1) {
                        if (appl != "wifi") updateUI()
                        Log.d(TAG, "updateLive: Message - $msg")
                    } else {
                        loadingDialog.dismiss()
                        showPSnackbar("Failed to update room")
                        Log.e(TAG, "updateLive: Message - $msg")
                    }
                } catch (e: Exception) {
                    loadingDialog.dismiss()
                    Log.e(TAG, "Exception in updateLive: $e")
                    showLToast(e.message)
                }
            }, {
                loadingDialog.dismiss()
                showLToast("Something went wrong.")
                Log.e(TAG, "VollyError: ${it.message}")
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["device_id"] = currentDeviceId.toString()
                params["appliance"] = appl
                params["data"] = value
                return params
            }

            override fun getHeaders(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/x-www-form-urlencoded"
                return params
            }
        }

        requestQueue.add(liveUpdateRequest)
    }

    val wifiRunnable = Runnable {
        Log.i(TAG, "Runnable: Called")
        checkWifiIsRunning = false
        checkWifi = true
    }

    private fun togglePower(app1Val: String, app2Val: String, app3Val: String, app4Val: String,
        fan: String) {
        if (isAdded) {
            try {
                if (app1Val == ZERO && app2Val == ZERO && app3Val == ZERO && app4Val == ZERO && fan == ZERO) {
                    binding.powerBtn.setImageDrawable(
                        context?.let { ContextCompat.getDrawable(it, R.drawable.ic_powddr_btn_off) })
                    enableUI()
                    loadingDialog.dismiss()
                } else {
                    binding.powerBtn.setImageDrawable(
                        context?.let { ContextCompat.getDrawable(it, R.drawable.ic_power_btn_on) })
                    enableUI()
                    loadingDialog.dismiss()
                }
            } catch (e: Exception) {
                Log.e(TAG, "togglePower: Error", e)
            }
        }
//        else togglePower()
    }

    private fun togglePower() {
        val appliances = arrayOf(APPL1, APPL2, APPL3, APPL4, FAN)
        val toggleFlag = !binding.fanSwitch.isChecked && !binding.switch1Switch.isChecked
                && !binding.switch2Switch.isChecked && !binding.switch3Switch.isChecked
                && !binding.switch4Switch.isChecked

        for (appl in appliances) {
            updatePowerLive(if (toggleFlag) ONE else ZERO, appl)
        }
    }

    private fun updatePowerLive(value: String, appl: String) {

        val liveUpdateUrl = getString(R.string.base_url) + getString(R.string.url_live_update)

        val liveUpdateRequest = object : StringRequest(Method.POST, liveUpdateUrl,
            { response ->
                Log.i(TAG, "updateUI: $response")
                try {
                    val mData = JSONObject(response.toString())
                    val resp = mData.get("response") as Int
                    val msg = mData.get("msg")

                    if (resp == 1) {
                        if (appl == FAN) updateUI()
                        Log.d(TAG, "updateLive: Message - $msg")
                    } else {
                        loadingDialog.dismiss()
                        showPSnackbar("Failed to update room")
                        Log.e(TAG, "updateLive: Message - $msg")
                    }
                } catch (e: Exception) {
                    loadingDialog.dismiss()
                    Log.e(TAG, "Exception in updateLive: $e")
                    showLToast(e.message)
                }
            }, {
                loadingDialog.dismiss()
                showLToast("Something went wrong.")
                Log.e(TAG, "VollyError: ${it.message}")
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["device_id"] = currentDeviceId.toString()
                params["appliance"] = appl
                params["data"] = value
                return params
            }

            override fun getHeaders(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/x-www-form-urlencoded"
                return params
            }
        }

        requestQueue.add(liveUpdateRequest)
    }

    private fun gotoAddDevice() {
        Navigation.findNavController(requireView())
            .navigate(R.id.action_roomControlsFragment_to_addDeviceFragment)
    }

    private fun showChooseRoomDialog() {
        var selectedDevice = deviceIDList[selectedRoomIndex]
        var selectedRoom = roomsList[selectedRoomIndex]
        val rooms = roomsList.toTypedArray()

        MaterialAlertDialogBuilder(requireActivity())
            .setTitle("Rooms")
            .setSingleChoiceItems(rooms, selectedRoomIndex) { _, which ->
                selectedRoomIndex = which
                selectedRoom = roomsList[which]
                selectedDevice = deviceIDList[which]
            }
            .setPositiveButton("OK") { _, _ ->
                binding.currentRoomTv.text = selectedRoom
                currentDeviceId = selectedDevice
                checkDatabase()
//                uiHandler()
            }
            .setNeutralButton("Cancel") { _, _ -> }
            .show()
    }

    private fun showPopupMenu(view: View?, switchID: String, switchIDByApp: String) {
        val popup = PopupMenu(requireActivity(), view)
        popup.menuInflater.inflate(R.menu.switch_menu, popup.menu)
        popup.setOnMenuItemClickListener {
            when (it!!.itemId) {
                R.id.edit -> {
                    val intent = Intent(context, EditSwitchActivity::class.java)
                    intent.putExtra(ROOM_ID, currentDeviceId)
                    intent.putExtra(ROOM_NAME, roomsList[selectedRoomIndex])
                    intent.putExtra(SWITCH_ID, switchID)
                    intent.putExtra(SWITCH_ID_BY_APP, switchIDByApp)
                    startActivity(intent)
                }
            }
            true
        }
        popup.show()
    }

//    override fun onSaveInstanceState(outState: Bundle) {}

    private fun disableUI() {
        if (!isOnline()) showPSnackbar("No Internet Connection")
        else waitSnackbar.show()
        binding.powerBtn.isClickable = false
        binding.powerBtn.isEnabled = false
        binding.fanSwitch.isClickable = false
//        binding.fanSwitch.isEnabled = false
        binding.fanSpeedSlider.isClickable = false
        binding.fanSpeedSlider.isEnabled = false
        binding.switch1Switch.isClickable = false
        binding.switch1Switch.isEnabled = false
        binding.switch2Switch.isClickable = false
        binding.switch2Switch.isEnabled = false
        binding.switch3Switch.isClickable = false
        binding.switch3Switch.isEnabled = false
        binding.switch4Switch.isClickable = false
        binding.switch4Switch.isEnabled = false
    }

    private fun enableUI() {
        if (isOnline()) {
            waitSnackbar.dismiss()
//            loadingDialog.dismiss()
            binding.powerBtn.isClickable = true
            binding.powerBtn.isEnabled = true
            binding.fanSwitch.isClickable = true
            binding.fanSwitch.isEnabled = true
            binding.fanSpeedSlider.isClickable = true
            binding.fanSpeedSlider.isEnabled = true
            binding.switch1Switch.isClickable = true
            binding.switch1Switch.isEnabled = true
            binding.switch2Switch.isClickable = true
            binding.switch2Switch.isEnabled = true
            binding.switch3Switch.isClickable = true
            binding.switch3Switch.isEnabled = true
            binding.switch4Switch.isClickable = true
            binding.switch4Switch.isEnabled = true
        } else showPSnackbar("No Internet Connection")
    }

    private fun isOnline(): Boolean {
        if (context != null) {
            val connectivityManager =
                context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
        }
        return false
    }

    private fun showLToast(message: String?) {
        Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show()
    }

    private fun showSToast(message: String?) {
        Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show()
    }

    /*private fun showSSnackbar(msg: String = "Please wait...") {
        Snackbar.make(binding.rcRootView, msg, Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.rcRootView)
//            .setAction("Retry") {
//
//            }
            .show()
    }*/

    private fun showPSnackbar(msg: String = "Something went wrong.") {
        if (context != null) {
            Snackbar.make(binding.rcRootView, msg, Snackbar.LENGTH_INDEFINITE)
                .setAction("Retry") {
                    if (isOnline()) refreshUI()
                    else showPSnackbar(msg)
                }
                .show()
        } else {
            Log.e(TAG, "showPSnackbar: Contect Error - $context")
            checkDatabase()
        }
    }

    private fun refreshUI() {
        loadingDialog = LoadingDialog()
        loadingDialog.isCancelable = false

        if (!currentDeviceId.isNullOrBlank() || !currentDeviceId.equals("null")) checkDatabase()
        else {
            Log.i(TAG, "onViewCreated: ~~~~ $currentDeviceId is not present")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume: Called $currentDeviceId")
        refreshUI()
    }
}