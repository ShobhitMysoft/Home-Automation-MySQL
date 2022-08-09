package com.mysofttechnology.homeautomation

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.TypedArray
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
import com.mysofttechnology.homeautomation.activities.EditSwitchActivity.Companion.USER_ID
import com.mysofttechnology.homeautomation.database.Device
import com.mysofttechnology.homeautomation.databinding.FragmentRoomControlsBinding
import com.mysofttechnology.homeautomation.models.DeviceViewModel
import com.mysofttechnology.homeautomation.utils.VolleySingleton
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStream
import java.util.*

private const val TAG = "RoomControlsFragment"

class RoomControlsFragment : Fragment() {

    private var ssidOutputStream: OutputStream? = null
    private var isLoadingUi: Boolean = false
    private var powerBtnClicked: Boolean = false
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var isBTConnected: Boolean = false
    private lateinit var deviceViewModel: DeviceViewModel

    //    private lateinit var allData: List<Device>
    private var btSocket: BluetoothSocket? = null
    private val mUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var liveFanSpeed: Int = 0
    private var SWITCH1: String? = null
    private var SWITCH2: String? = null
    private var SWITCH3: String? = null
    private var SWITCH4: String? = null
    private var SWITCH6: String? = null
    private val CHECK_WIFI_DELAY_TIME: Long = 4000
    private val CHECK_BT_DELAY_TIME: Long = 5000
    private lateinit var toggleWifi: Handler
    private lateinit var btHandler: Handler
    private var checkWifiIsRunning: Boolean = false
    private var checkWifi: Boolean = false
    private var sharedPref: SharedPreferences? = null
    private lateinit var requestQueue: RequestQueue
    private lateinit var waitSnackbar: Snackbar

    private var currentDeviceId: String? = null
    private var curDevSwitchCount: String? = "5"
    private lateinit var cd: Device
    private var currentUserId: String? = null
    private var currentBtDeviceId: String? = null

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

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            exitAppDialog.show(childFragmentManager, "Exit App")
        }

        callback.isEnabled = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        _binding = FragmentRoomControlsBinding.inflate(inflater, container, false)

        iconsList = resources.obtainTypedArray(R.array.icons_list)

        deviceViewModel = ViewModelProvider(this).get(DeviceViewModel::class.java)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestQueue = VolleySingleton.getInstance(requireContext()).requestQueue
        sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE) ?: return

        currentUserId = sharedPref!!.getString(getString(R.string.current_user_id), "")
        selectedRoomIndex = sharedPref!!.getInt(getString(R.string.selected_room_index), 0)

        Log.d(TAG, "onViewCreated: $currentUserId")

        toggleWifi = Handler(Looper.getMainLooper())
        btHandler = Handler(Looper.getMainLooper())
        waitSnackbar =
            Snackbar.make(requireActivity().findViewById(android.R.id.content), "Please wait...",
                Snackbar.LENGTH_INDEFINITE)

        val bluetoothManager =
            activity?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        binding.currentRoomTv.setOnClickListener {
            showChooseRoomDialog()
        }

        binding.switch1Card.setOnLongClickListener {
            Toast.makeText(requireContext(), "Long press detected", Toast.LENGTH_SHORT).show()
            true
        }

        uiHandler()
        loadUi()
//        connectToBtDevice()
    }

    private fun uiHandler() {
        Log.d(TAG, "uiHandler: Called\n")
        val spEditor = sharedPref?.edit()

        binding.powerBtn.setOnClickListener {
            powerBtnClicked = true
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
                val speed = slider.value
                disableUI()
                if (isBTConnected) sendDataToBT(when (speed) {
                    1.0f -> "F"
                    2.0f -> "G"
                    3.0f -> "H"
                    4.0f -> "I"
                    else -> "E"
                })
                else {
                    if (!checkWifiIsRunning) {
                        checkWifiIsRunning = true
                        toggleWifi.postDelayed(wifiRunnable, CHECK_WIFI_DELAY_TIME)
                    }
                    updateLive(speed.toInt().toString(), FAN)
                }
                spEditor?.putString("old_fan_speed_$currentDeviceId)", speed.toInt().toString())
                spEditor?.apply()
            }
        })

        binding.fanSwitch.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "uiHandler: fanSwitch isChecked Called")
            if (!isLoadingUi) {
                disableUI()
                if (isBTConnected) {
                    if (!isChecked) {
                        sendDataToBT("E")
                    } else {
                        val oldFanSpeed = sharedPref!!.getString("old_fan_speed_$currentDeviceId)",
                            liveFanSpeed.toString())
                        sendDataToBT(when (oldFanSpeed) {
                            "1" -> "F"
                            "2" -> "G"
                            "3" -> "H"
                            "4" -> "I"
                            else -> "E"
                        })
                    }
                } else {
                    if (!checkWifiIsRunning) {
                        checkWifiIsRunning = true
                        toggleWifi.postDelayed(wifiRunnable, CHECK_WIFI_DELAY_TIME)
                    }
                    if (!isChecked) {
                        updateLive(ZERO, FAN)
                    } else {
                        val oldFanSpeed = sharedPref!!.getString("old_fan_speed_$currentDeviceId)",
                            liveFanSpeed.toString())
                        updateLive(oldFanSpeed.toString(), FAN)
                    }
                }
            }
        }

        binding.switch1Switch.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "uiHandler: switch1Switch isChecked Called")
            if (!isLoadingUi) {
                disableUI()
                if (isBTConnected) sendDataToBT(
                    if (isChecked) "A" else "a")                            // TODO: Step 8
                else {
                    if (!checkWifiIsRunning) {
                        checkWifiIsRunning = true
                        toggleWifi.postDelayed(wifiRunnable, CHECK_WIFI_DELAY_TIME)
                    }
                    updateLive(if (isChecked) ONE else ZERO, APPL1)
                }
            }
        }

        binding.switch2Switch.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "uiHandler: switch2Switch isChecked Called")
            if (!isLoadingUi) {
                disableUI()
                if (isBTConnected) sendDataToBT(
                    if (isChecked) "B" else "b")                            // TODO: Step 8
                else {
                    if (!checkWifiIsRunning) {
                        checkWifiIsRunning = true
                        toggleWifi.postDelayed(wifiRunnable, CHECK_WIFI_DELAY_TIME)
                    }
                    updateLive(if (isChecked) ONE else ZERO, APPL2)
                }
            }
        }

        binding.switch3Switch.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "uiHandler: switch3Switch isChecked Called")
            if (!isLoadingUi) {
                disableUI()
                if (isBTConnected) sendDataToBT(
                    if (isChecked) "C" else "c")                            // TODO: Step 8
                else {
                    if (!checkWifiIsRunning) {
                        checkWifiIsRunning = true
                        toggleWifi.postDelayed(wifiRunnable, CHECK_WIFI_DELAY_TIME)
                    }
                    updateLive(if (isChecked) ONE else ZERO, APPL3)
                }
            }
        }

        binding.switch4Switch.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "uiHandler: switch4Switch isChecked Called")
            if (!isLoadingUi) {
                disableUI()
                if (isBTConnected) sendDataToBT(
                    if (isChecked) "D" else "d")                            // TODO: Step 8
                else {
                    if (!checkWifiIsRunning) {
                        checkWifiIsRunning = true
                        toggleWifi.postDelayed(wifiRunnable, CHECK_WIFI_DELAY_TIME)
                    }
                    updateLive(if (isChecked) ONE else ZERO, APPL4)
                }
            }
        }

        binding.switch6Switch.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "uiHandler: switch6Switch isChecked Called")
            if (!isLoadingUi) {
                disableUI()
                if (isBTConnected) sendDataToBT(
                    if (isChecked) "A" else "a")                            // TODO: Step 8
                else {
                    if (!checkWifiIsRunning) {
                        checkWifiIsRunning = true
                        toggleWifi.postDelayed(wifiRunnable, CHECK_WIFI_DELAY_TIME)
                    }
                    updateLive(if (isChecked) ONE else ZERO, APPL1)
                }
            }
        }

        binding.switch1MoreBtn.setOnClickListener {
            if (SWITCH1 != null) showPopupMenu(it, SWITCH1!!, "1")
            else if (isOnline()) updateUI()
            else showSToast("You are offline")
        }
        binding.switch2MoreBtn.setOnClickListener {
            if (SWITCH2 != null) showPopupMenu(it, SWITCH2!!, "2")
            else if (isOnline()) updateUI()
            else showSToast("You are offline")
        }
        binding.switch3MoreBtn.setOnClickListener {
            if (SWITCH3 != null) showPopupMenu(it, SWITCH3!!, "3")
            else if (isOnline()) updateUI()
            else showSToast("You are offline")
        }
        binding.switch4MoreBtn.setOnClickListener {
            if (SWITCH4 != null) showPopupMenu(it, SWITCH4!!, "4")
            else if (isOnline()) updateUI()
            else showSToast("You are offline")
        }
        binding.switch6MoreBtn.setOnClickListener {
            if (SWITCH6 != null) showPopupMenu(it, SWITCH6!!, "6")
            else if (isOnline()) updateUI()
            else showSToast("You are offline")
        }
    }

    private fun loadUi() {
        loadingDialog = LoadingDialog()
        loadingDialog.isCancelable = false
        isLoadingUi = true

        if (!currentDeviceId.isNullOrBlank() || !currentDeviceId.equals("null")) {
//            checkDatabase()
            checkLocalDatabase()                                // Load UI
        } else {
            Log.i(TAG, "onViewCreated: ~~~~ $currentDeviceId is not present")
        }
    }

    private fun checkLocalDatabase() {                                                              // TODO: Step 4
//        binding.mainControlsView.visibility = View.INVISIBLE
        isLoadingUi = true
        var i = 0
        val allData = deviceViewModel.readAllData

        allData.observe(viewLifecycleOwner) { deviceList ->
            Log.i(TAG, "checkLocalDatabase: ${deviceList.size}")
            roomsList.clear()
            deviceIDList.clear()
            deviceList.forEach {
                Log.d(TAG, "checkLocalDatabase ${++i}: ${it.name} | ${it.bluetoothId}")
                roomsList.add(it.name)
                deviceIDList.add(it.deviceId)
            }

            if (deviceIDList.size > 0) {
                try {
                    Log.d(TAG, "checkLocalDatabase: ${allData.value}")
                    if (selectedRoomIndex >= deviceIDList.size) selectedRoomIndex = 0

                    cd = allData.value?.get(selectedRoomIndex)!!
                    currentDeviceId = cd.deviceId
                    curDevSwitchCount = cd.switchCount
                    currentBtDeviceId = cd.bluetoothId
                    binding.currentRoomTv.text = cd.name
                    if (isOnline()) connectToInternet() else updateUIWithLocalDB()
                } catch (e: Exception) {
                    Log.e(TAG, "checkLocalDatabase: $roomsList\n$deviceIDList")
                    Log.e(TAG, "checkLocalDatabase: Error", e)
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun updateUIWithLocalDB() {                                                             // TODO: Step 5
        Log.d(TAG, "checkLocalDatabase: Called")
        binding.connectionBtn.visibility = View.INVISIBLE
        binding.statusPb.visibility = View.VISIBLE

        if (curDevSwitchCount == "1") {
            binding.sl1View.visibility = View.VISIBLE
            binding.sl5View.visibility = View.GONE
            binding.powerBtnView.visibility = View.GONE

            iconsList = resources.obtainTypedArray(R.array.sl1_icons_list)

            binding.switch6Switch.isChecked = cd.s6State == 1
            binding.switch6Name.text = cd.s6Name
            binding.switch6Icon.setImageResource(iconsList.getResourceId(cd.s6Icon, 0))
        } else {
            binding.sl1View.visibility = View.GONE
            binding.sl5View.visibility = View.VISIBLE
            binding.powerBtnView.visibility = View.VISIBLE

            iconsList = resources.obtainTypedArray(R.array.icons_list)

            binding.switch1Switch.isChecked = cd.s1State == 1
            binding.switch2Switch.isChecked = cd.s2State == 1
            binding.switch3Switch.isChecked = cd.s3State == 1
            binding.switch4Switch.isChecked = cd.s4State == 1

            binding.switch1Name.text = cd.s1Name
            binding.switch2Name.text = cd.s2Name
            binding.switch3Name.text = cd.s3Name
            binding.switch4Name.text = cd.s4Name

            binding.switch1Icon.setImageResource(iconsList.getResourceId(cd.s1Icon, 0))
            binding.switch2Icon.setImageResource(iconsList.getResourceId(cd.s2Icon, 0))
            binding.switch3Icon.setImageResource(iconsList.getResourceId(cd.s3Icon, 0))
            binding.switch4Icon.setImageResource(iconsList.getResourceId(cd.s4Icon, 0))

            if (cd.fanSpeed == 0) {
                binding.fanSpeedSlider.value = 0.0f
                binding.fanSpeedTv.text = ZERO
                if (binding.fanSwitch.isChecked) binding.fanSwitch.isChecked = false
            } else {
                binding.fanSpeedSlider.value = cd.fanSpeed.toFloat()
                binding.fanSpeedTv.text = cd.fanSpeed.toString()
                if (!binding.fanSwitch.isChecked) binding.fanSwitch.isChecked = true
            }

            togglePower(cd.s1State.toString(), cd.s2State.toString(), cd.s3State.toString(),
                cd.s4State.toString(), cd.fanSpeed.toString())
        }

        Log.i(TAG, "updateUIWithLocalDB: Data updated from Local")

        checkBluetooth()
    }

    private fun checkBluetooth() {
        if (bluetoothAdapter?.isEnabled == true && currentBtDeviceId != null && currentBtDeviceId != "null") {
//            binding.mainControlsView.visibility = View.VISIBLE
            GlobalScope.launch(Dispatchers.IO) {
                connectToBtDevice()
            }
        }
    }

    private suspend fun connectToBtDevice() {                                                               // TODO: Step 6
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        val remoteDevice = btAdapter.getRemoteDevice(currentBtDeviceId)
        
        closeSocket()
        btSocket = remoteDevice.createRfcommSocketToServiceRecord(mUUID)

        try {
            Log.i(TAG, "connectToBtDevice: Try ${Calendar.getInstance().time}")
            btHandler.postDelayed(btRunnable, CHECK_BT_DELAY_TIME)
            btSocket!!.connect()
            Log.i(TAG, "connectToBtDevice: Try complete ${Calendar.getInstance().time}")
        } catch (e: Exception) {
            checkBtIsConnected()
            closeSocket()   // failed to connectToBtDevice
        }

        checkBtIsConnected()
    }

    private fun checkBtIsConnected() {
        if (btSocket?.isConnected == true) {                                                        // TODO: Step 7
            isBTConnected = true
            ssidOutputStream = btSocket!!.outputStream
            requireActivity().runOnUiThread {
                try {
                    binding.connectionBtn.setImageDrawable(
                        context?.let { ContextCompat.getDrawable(it, R.drawable.ic_bluetooth_on) })
                    binding.statusPb.visibility = View.INVISIBLE
                    binding.connectionBtn.visibility = View.VISIBLE
                } catch (e: Exception) {
                    Log.e(TAG, "connectToBtDevice isBTConnected = $isBTConnected: Error", e)
                }
//                binding.mainControlsView.visibility = View.VISIBLE
            }
        } else isBTConnected = false
    }

    private fun connectToInternet() {                                                               // TODO: Step 6
        checkBluetooth()
//        binding.mainControlsView.visibility = View.VISIBLE
        if (isOnline()) {                                                                           // TODO: Step 9.1
            try {
                binding.connectionBtn.setImageDrawable(
                    context?.let { ContextCompat.getDrawable(it, R.drawable.ic_network) })
                updateUI()
                binding.statusPb.visibility = View.INVISIBLE
                binding.connectionBtn.visibility = View.VISIBLE
            } catch (e: Exception) {
                Log.e(TAG, "connectToInternet: Error", e)
            }
        } else {                                                                           // TODO: Step 9.1
            try {
                binding.connectionBtn.setImageDrawable(
                    context?.let { ContextCompat.getDrawable(it, R.drawable.ic_no_network) })
                enableUI()
                binding.statusPb.visibility = View.INVISIBLE
                binding.connectionBtn.visibility = View.VISIBLE
            } catch (e: Exception) {
                Log.e(TAG, "connectToInternet: Error", e)
            }
        }
    }

    private fun closeSocket() {
        try {
            btSocket?.close()
            btSocket = null
        } catch (e: IOException) {
            Log.e(TAG, "connectToBtDevice: Socket Close Error : ", e)
        }
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
                        if (curDevSwitchCount == "1") {
                            val app6Val = mData.get(APPL1).toString()
                            val wifi = mData.get("wifi").toString()

                            if (checkWifi && wifi == "0") showDeviceOfflineDialog()
                            else {
                                updateLive("0", "wifi")
                                binding.switch6Switch.isChecked = app6Val == ONE

                                checkWifi = false
                            }
                        } else {
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
                                    if (binding.fanSwitch.isChecked) binding.fanSwitch.isChecked =
                                        false
                                } else {
                                    binding.fanSpeedSlider.value = liveFanSpeed.toFloat()
                                    binding.fanSpeedTv.text = fan
                                    if (!binding.fanSwitch.isChecked) binding.fanSwitch.isChecked =
                                        true
                                }

                                checkWifi = false
                                togglePower(app1Val, app2Val, app3Val, app4Val, fan)
                            }
                        }

                        Log.d(TAG, "updateUI: Message - $msg")
                    } else {


                        showPSnackbar("Failed to get room data")
                        Log.e(TAG, "updateUI: Message - $msg")
                    }
                } catch (e: Exception) {


                    Log.e(TAG, "Exception in updateUI: $e")
                    if (e.message != null) showLToast("${e.message}")
                }
            }, {


                showLToast("Something went wrong.")
                Log.e(TAG, "VollyError: ${it.message}")
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["device_id"] = currentDeviceId.toString()
                params["mobile_no"] = currentUserId.toString()
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

                        if (curDevSwitchCount == "1") {
                            val switchData = switchListData.getJSONObject(0)
                            loadSwitch(switchData.get("switch_id_by_app").toString(), switchData)
                        } else {
                            for (i in 0..4) {
                                val switchData = switchListData.getJSONObject(i)
                                if (switchData.get("switch_id_by_app").toString() != "5")
                                    loadSwitch(switchData.get("switch_id_by_app").toString(),
                                        switchData)
                            }
                        }
                        Log.d(TAG, "switchList: Message - $msg")
                    } else {


                        showPSnackbar("Failed to get switch data")
                        Log.e(TAG, "switch switchList: Message - $msg")
                    }
                } catch (e: Exception) {


                    Log.e(TAG, "Exception in switch updateUI: $e")
                    if (e.message != null) showLToast("${e.message}")
                }
            }, {


                showLToast("Something went wrong.")
                Log.e(TAG, "VollyError: ${it.message}")
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["device_id"] = currentDeviceId.toString()
                params["mobile_no"] = currentUserId.toString()
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
            "6" -> SWITCH6 = switch.get("id").toString()
            else -> SWITCH4 = switch.get("id").toString()
        }
        val switchName = when (switchId) {
            "1" -> binding.switch1Name
            "2" -> binding.switch2Name
            "3" -> binding.switch3Name
            "6" -> binding.switch6Name
            else -> binding.switch4Name
        }
        val switchIcon = when (switchId) {
            "1" -> binding.switch1Icon
            "2" -> binding.switch2Icon
            "3" -> binding.switch3Icon
            "6" -> binding.switch6Icon
            else -> binding.switch4Icon
        }
        val switchStartTime = when (switchId) {
            "1" -> binding.switch1StartTimeTv
            "2" -> binding.switch2StartTimeTv
            "3" -> binding.switch3StartTimeTv
            "6" -> binding.switch6StartTimeTv
            else -> binding.switch4StartTimeTv
        }
        val switchStopTime = when (switchId) {
            "1" -> binding.switch1StopTimeTv
            "2" -> binding.switch2StopTimeTv
            "3" -> binding.switch3StopTimeTv
            "6" -> binding.switch6StopTimeTv
            else -> binding.switch4StopTimeTv
        }

        val sunTv = when (switchId) {
            "1" -> binding.switch1SunTv
            "2" -> binding.switch2SunTv
            "3" -> binding.switch3SunTv
            "6" -> binding.switch6SunTv
            else -> binding.switch4SunTv
        }
        val monTv = when (switchId) {
            "1" -> binding.switch1MonTv
            "2" -> binding.switch2MonTv
            "3" -> binding.switch3MonTv
            "6" -> binding.switch6MonTv
            else -> binding.switch4MonTv
        }
        val tueTv = when (switchId) {
            "1" -> binding.switch1TueTv
            "2" -> binding.switch2TueTv
            "3" -> binding.switch3TueTv
            "6" -> binding.switch6TueTv
            else -> binding.switch4TueTv
        }
        val wedTv = when (switchId) {
            "1" -> binding.switch1WedTv
            "2" -> binding.switch2WedTv
            "3" -> binding.switch3WedTv
            "6" -> binding.switch6WedTv
            else -> binding.switch4WedTv
        }
        val thuTv = when (switchId) {
            "1" -> binding.switch1ThuTv
            "2" -> binding.switch2ThuTv
            "3" -> binding.switch3ThuTv
            "6" -> binding.switch6ThuTv
            else -> binding.switch4ThuTv
        }
        val friTv = when (switchId) {
            "1" -> binding.switch1FriTv
            "2" -> binding.switch2FriTv
            "3" -> binding.switch3FriTv
            "6" -> binding.switch6FriTv
            else -> binding.switch4FriTv
        }
        val satTv = when (switchId) {
            "1" -> binding.switch1SatTv
            "2" -> binding.switch2SatTv
            "3" -> binding.switch3SatTv
            "6" -> binding.switch6SatTv
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

        checkForNotification(switch.get("id").toString(), switch.getString(SWITCH),
            switch.getString(NOTIFICATION))
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


                        showLToast("unable to create room")
                        Log.e(TAG, "updateSwitch: Message - $msg")
                    }
                } catch (e: Exception) {


                    Log.e(TAG, "Exception in updateSwitch: $e")
                }
            }, {


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

    private fun sendDataToBT(signal: String) {
        Log.d(TAG, "sendDataToBT: Called $signal")
        if (btSocket?.isConnected == true) {
            try {
                ssidOutputStream?.write(signal.toByteArray())

                when (signal) {
                    "F" -> {
                        binding.fanSpeedSlider.value = 1.0f
                        binding.fanSpeedTv.text = "1"
                        if (!binding.fanSwitch.isChecked) binding.fanSwitch.isChecked = true
                    }
                    "G" -> {
                        binding.fanSpeedSlider.value = 2.0f
                        binding.fanSpeedTv.text = "2"
                        if (!binding.fanSwitch.isChecked) binding.fanSwitch.isChecked = true
                    }
                    "H" -> {
                        binding.fanSpeedSlider.value = 3.0f
                        binding.fanSpeedTv.text = "3"
                        if (!binding.fanSwitch.isChecked) binding.fanSwitch.isChecked = true
                    }
                    "I" -> {
                        binding.fanSpeedSlider.value = 4.0f
                        binding.fanSpeedTv.text = "4"
                        if (!binding.fanSwitch.isChecked) binding.fanSwitch.isChecked = true
                    }
                    "E" -> {
                        binding.fanSpeedSlider.value = 0.0f
                        binding.fanSpeedTv.text = "0"
                        if (binding.fanSwitch.isChecked) binding.fanSwitch.isChecked = false
                    }
                }

                enableUI()

//            closeSocket()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else showSToast("Bluetooth not connected.")

    }

    private fun updateLive(value: String, appl: String) {
        if (isOnline()) {
            val liveUpdateUrl = getString(R.string.base_url) + getString(R.string.url_live_update)

            val liveUpdateRequest = object : StringRequest(Method.POST, liveUpdateUrl,
                { response ->
                    Log.i(TAG, "updateUI: $response")
                    try {
                        val mData = JSONObject(response.toString())
                        val resp = mData.get("response") as Int
                        val msg = mData.get("msg")

                        if (resp == 1) {
                            if (appl != "wifi" && isLoadingUi) updateUI()
                            Log.d(TAG, "updateLive: Message - $msg")
                        } else {


                            showPSnackbar("Failed to update room")
                            Log.e(TAG, "updateLive: Message - $msg")
                        }
                    } catch (e: Exception) {


                        Log.e(TAG, "Exception in updateLive: $e")
                        if (e.message != null) showLToast(e.message)
                    }
                }, {


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
        } else enableUI()
    }

    val wifiRunnable = Runnable {
        Log.i(TAG, "Runnable: Called")
        checkWifiIsRunning = false
        checkWifi = true
    }

    val btRunnable = Runnable {
        Log.i(TAG, "BT Runnable: Called ${Calendar.getInstance().time}")
//        closeSocket()
//        connectToInternet()
    }

    private fun togglePower(app1Val: String, app2Val: String, app3Val: String, app4Val: String,
        fan: String) {
        if (isAdded) {
            try {
                if (app1Val == ZERO && app2Val == ZERO && app3Val == ZERO && app4Val == ZERO && fan == ZERO) {
                    binding.powerBtn.setImageDrawable(
                        context?.let { ContextCompat.getDrawable(it, R.drawable.ic_power_btn_off) })
                    enableUI()

                } else {
                    binding.powerBtn.setImageDrawable(
                        context?.let { ContextCompat.getDrawable(it, R.drawable.ic_power_btn_on) })
                    enableUI()

                    Log.d(TAG, " ${Thread.currentThread().stackTrace[2].lineNumber - 1}")
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

        if (isBTConnected) {
            for (i in 0..4) {
                sendDataToBT(when (i) {
                    0 -> if (toggleFlag) "A" else "a"
                    1 -> if (toggleFlag) "B" else "b"
                    2 -> if (toggleFlag) "C" else "c"
                    3 -> if (toggleFlag) "D" else "d"
                    else -> if (toggleFlag) {
                        val oldFanSpeed = sharedPref!!.getString("old_fan_speed_$currentDeviceId)",
                            liveFanSpeed.toString())
                        val fanSpeed = when (oldFanSpeed) {
                            "1" -> "F"
                            "2" -> "G"
                            "3" -> "H"
                            "4" -> "I"
                            else -> "E"
                        }
                        fanSpeed
                    } else "E"
                })
            }
        } else if (isOnline()) {
            for (appl in appliances) {
                updatePowerLive(if (toggleFlag) ONE else ZERO, appl)
            }
        } else {
            showLToast("You are not connected.")
        }
        if (powerBtnClicked) {
            powerBtnClicked = false
            loadingDialog.dismiss()
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


                        showPSnackbar("Failed to update room")
                        Log.e(TAG, "updateLive: Message - $msg")
                    }
                } catch (e: Exception) {


                    Log.e(TAG, "Exception in updateLive: $e")
                    if (e.message != null) showLToast(e.message)
                }
            }, {


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
//                closeSocket()
                binding.currentRoomTv.text = selectedRoom
                currentDeviceId = selectedDevice
                sharedPref?.edit()
                    ?.putInt(getString(R.string.selected_room_index), selectedRoomIndex)?.apply()
                checkLocalDatabase()                                // ChooseRoomDialog
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
                    intent.putExtra(USER_ID, currentUserId)
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
//        waitSnackbar.show()
        if (isAdded) {
            binding.powerBtn.isClickable = false
            binding.powerBtn.isEnabled = false
            binding.fanSwitch.isClickable = false
            binding.fanSpeedSlider.isClickable = false
//            binding.fanSpeedSlider.isEnabled = false
            binding.switch1Switch.isClickable = false
//            binding.switch1Switch.isEnabled = false
            binding.switch2Switch.isClickable = false
//            binding.switch2Switch.isEnabled = false
            binding.switch3Switch.isClickable = false
//            binding.switch3Switch.isEnabled = false
            binding.switch4Switch.isClickable = false
//            binding.switch4Switch.isEnabled = false
        }
    }

    private fun enableUI() {
//        waitSnackbar.dismiss()
        if (isAdded) {
            binding.powerBtn.isClickable = true
            binding.powerBtn.isEnabled = true
            binding.fanSwitch.isClickable = true
//            binding.fanSwitch.isEnabled = true
            binding.fanSpeedSlider.isClickable = true
//            binding.fanSpeedSlider.isEnabled = true
            binding.switch1Switch.isClickable = true
//            binding.switch1Switch.isEnabled = true
            binding.switch2Switch.isClickable = true
//            binding.switch2Switch.isEnabled = true
            binding.switch3Switch.isClickable = true
//            binding.switch3Switch.isEnabled = true
            binding.switch4Switch.isClickable = true
//            binding.switch4Switch.isEnabled = true
        }
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

    private fun showLToast(message: String? = "Message") {
        if (isAdded) Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show()
    }

    private fun showSToast(message: String? = "Message") {
        if (isAdded) Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showPSnackbar(msg: String = "Something went wrong.") {
        if (context != null) {
            Snackbar.make(binding.rcRootView, msg, Snackbar.LENGTH_INDEFINITE)
                .setAction("Retry") {
                    if (isOnline()) loadUi()
                    else showPSnackbar(msg)
                }
                .show()
        } else {
            Log.e(TAG, "showPSnackbar: Contect Error - $context")
//            checkDatabase()
            checkLocalDatabase()                                // Snack bar Retry
        }
    }

//    override fun onResume() {
//        super.onResume()
//        Log.i(TAG, "onResume: Called $currentDeviceId")
////        if (!isOnline()) showSToast("No Internet Connection")
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        closeSocket()
    }

    override fun onDestroy() {
        super.onDestroy()
        closeSocket()
    }
}