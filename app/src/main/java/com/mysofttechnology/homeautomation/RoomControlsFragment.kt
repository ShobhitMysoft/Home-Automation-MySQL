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
import com.mysofttechnology.homeautomation.activities.EditSwitchActivity.Companion.SWITCH_ID
import com.mysofttechnology.homeautomation.activities.ErrorActivity
import com.mysofttechnology.homeautomation.databinding.FragmentRoomControlsBinding
import com.mysofttechnology.homeautomation.utils.VolleySingleton
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "RoomControlsFragment"

class RoomControlsFragment : Fragment() {

    private lateinit var toggleWifi: Handler
    private var checkWifiIsRunning: Boolean = false
    private var checkWifi: Boolean = false
    private var sharedPref: SharedPreferences? = null
    private lateinit var requestQueue: RequestQueue
    private lateinit var waitSnackbar: Snackbar

    //    private var valueEventListener: ValueEventListener? = null
    private var currentDeviceId: String? = null
    private var currentUserId: String? = null

    private var _binding: FragmentRoomControlsBinding? = null
    private val binding get() = _binding!!

    private lateinit var loadingDialog: LoadingDialog

    //    private lateinit var myFD: MyFirebaseDatabase
//    private lateinit var profileDBRef: DatabaseReference
//    private lateinit var devicesDBRef: DatabaseReference
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

//        myFD = MyFirebaseDatabase()
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
        loadingDialog.show(childFragmentManager, TAG)
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
                        Log.e(TAG, "Exception: $e")
                        showToast(e.message)
                        gotoAddDevice()
                    }
                }, {
                    loadingDialog.dismiss()
                    showToast("Something went wrong.")
                    gotoAddDevice()
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
            showLSnackbar("No internet connection")
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


        /*profileDBRef.child(DEVICES).get().addOnSuccessListener {

            Log.d(TAG, "checkDatabase: ${it.childrenCount}")

            it.children.forEach { device ->
                val devId = device.child("id").value
                if (devId != null && devId.toString() != "null") {
                    roomsList.add(device.child("name").value.toString())
                    deviceIDList.add(devId.toString())
                } else profileDBRef.child(DEVICES).child(device.key.toString()).removeValue()
            }

            Log.i(TAG, "checkDatabase: $deviceIDList")
            if (deviceIDList.isEmpty()) checkDatabase()
            else currentDeviceId = deviceIDList[selectedRoomIndex]

            if (currentDeviceId.isNullOrBlank() or roomsList.isEmpty() or deviceIDList.isEmpty()) {
                if (checkDBCounter > 2) {
//                    loadingDialog.dismiss()
                    binding.rcRootView.visibility = View.INVISIBLE
                    Snackbar.make(binding.rcRootView, "Something went wrong",
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction("Retry") {
                            Navigation.findNavController(requireView())
                                .navigate(R.id.action_roomControlsFragment_to_addDeviceFragment)
                        }
                        .show()
                } else {
                    checkDatabase()
                    checkDBCounter++
                }
            } else {
                binding.currentRoomTv.text = roomsList[selectedRoomIndex]
                updateUI()
            }
        }*/
    }

    /*
//    private fun databaseHandler() {
//        Log.d(TAG, "databaseHandler: Called")
////        devicesDBRef.child(currentDeviceId).child("oldFanSpeed").setValue(ZERO)
//
//        currentDeviceId?.let {
//            valueEventListener =
//                devicesDBRef.child(it).addValueEventListener(object : ValueEventListener {
//                    override fun onDataChange(snapshot: DataSnapshot) {
//                        valueEventListener?.let { vel -> devicesDBRef.removeEventListener(vel) }
//                        devicesDBRef.child(it).get().addOnSuccessListener { device ->
//                            try {
//                                val app1Val = device.child(APPL1).value.toString()
//                                val app2Val = device.child(APPL2).value.toString()
//                                val app3Val = device.child(APPL3).value.toString()
//                                val app4Val = device.child(APPL4).value.toString()
//                                val fan = device.child(FAN).value.toString()
//                                val fanSpeed = fan.toInt()
//
//                                binding.switch1Switch.isChecked = app1Val == ONE
//
//                                binding.switch2Switch.isChecked = app2Val == ONE
//
//                                binding.switch3Switch.isChecked = app3Val == ONE
//
//                                binding.switch4Switch.isChecked = app4Val == ONE
//
//                                if (fanSpeed == 0) {
//                                    binding.fanSpeedSlider.value = 0.0f
//                                    binding.fanSpeedTv.text = ZERO
//                                    binding.fanSwitch.isChecked = false
//                                } else {
//                                    binding.fanSpeedSlider.value = fanSpeed.toFloat()
//                                    binding.fanSpeedTv.text = fan
//                                    binding.fanSwitch.isChecked = true
//                                }
//
//                                togglePower(app1Val, app2Val, app3Val, app4Val, fan)
//                            } catch (e: Exception) {
//                                showErrorScreen()
////                            showLSnackbar()
//                                Log.e(TAG, "onDataChange: Database Error", e)
//                            }
//                        }
//
////                        loadingDialog.dismiss()
//                    }
//
//                    override fun onCancelled(error: DatabaseError) {
//                        Log.e(TAG, "onCancelled: databaseHandler() - ${error.message}")
////                        loadingDialog.dismiss()
//                    }
//                })
//        }
////        loadingDialog.dismiss()
//
//        uiHandler()
//    }

     */

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
                            val fanSpeed = fan.toInt()

                            binding.switch1Switch.isChecked = app1Val == ONE
                            binding.switch2Switch.isChecked = app2Val == ONE
                            binding.switch3Switch.isChecked = app3Val == ONE
                            binding.switch4Switch.isChecked = app4Val == ONE

                            if (fanSpeed == 0) {
                                binding.fanSpeedSlider.value = 0.0f
                                binding.fanSpeedTv.text = ZERO
                                if (binding.fanSwitch.isChecked) binding.fanSwitch.isChecked = false
                            } else {
                                binding.fanSpeedSlider.value = fanSpeed.toFloat()
                                binding.fanSpeedTv.text = fan
                                if (!binding.fanSwitch.isChecked) binding.fanSwitch.isChecked = true
                            }

                            checkWifi = false
                            togglePower(app1Val, app2Val, app3Val, app4Val, fan)
                        }

                        Log.d(TAG, "updateUI: Message - $msg")
                    } else {
                        loadingDialog.dismiss()
                        // TODO: Show snackbar to retry
                        showToast("unable to get data")
//                        showErrorScreen()
                        Log.e(TAG, "updateUI: Message - $msg")
                    }
                } catch (e: Exception) {
                    loadingDialog.dismiss()
                    Log.e(TAG, "Exception in updateUI: $e")
                    showToast(e.message)
                }
            }, {
                loadingDialog.dismiss()
                showToast("Something went wrong.")
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
                Log.i(TAG, "updateUI: $response")
                try {
                    val mData = JSONObject(response.toString())
                    val resp = mData.get("response") as Int
                    val msg = mData.get("msg")

                    if (resp == 1) {
                        val switchListData = mData.get("data") as JSONArray
                        for (i in 1..4) {
                            val switchData = switchListData.getJSONObject(i)
                            updateSwitch(switchData.get("switch_id_by_app").toString(), switchData)
//                            if (switchList.get("switch_id_by_app") == i) {
//
//                            }
                        }
                        Log.d(TAG, "updateUI: Message - $msg")
                    } else {
                        loadingDialog.dismiss()
                        // TODO: Show snackbar to retry
//                        showToast("unable to get data")
//                        showErrorScreen()
                        Log.e(TAG, "switch updateUI: Message - $msg")
                    }
                } catch (e: Exception) {
                    loadingDialog.dismiss()
                    Log.e(TAG, "Exception in switch updateUI: $e")
                    showToast(e.message)
                }
            }, {
                loadingDialog.dismiss()
                showToast("Something went wrong.")
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

    private fun updateSwitch(switchId: String, switch: JSONObject) {
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

        switchStartTime.text = switch.getString(START_TIME)
        switchStopTime.text = switch.getString(STOP_TIME)

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
    }

    private fun uiHandler() {
        Log.d(TAG, "uiHandler: Called\n")
        val spEditor = sharedPref?.edit()

        binding.powerBtn.setOnClickListener {
            loadingDialog.show(childFragmentManager, TAG)
            // TODO: Check DATABASE Wifi value
//            if (checkWifiIsRunning) toggleWifi.postDelayed(wifiRunnable, 10000)
            togglePower()
        }

        binding.fanSpeedSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}

            override fun onStopTrackingTouch(slider: Slider) {
                disableUI()
                if (!checkWifiIsRunning) {
                    checkWifiIsRunning = true
                    toggleWifi.postDelayed(wifiRunnable, 10000)
                }
                val speed = slider.value

                updateLive(speed.toInt().toString(), FAN)

                spEditor?.putString("old_fan_speed_$currentDeviceId)", speed.toInt().toString())
                spEditor?.apply()

                /*currentDeviceId?.let {
                    devicesDBRef.child(it).child("fan").setValue(speed.toInt().toString())
                        .addOnSuccessListener {
                            devicesDBRef.child(currentDeviceId!!).child("oldFanSpeed")
                                .setValue(if (speed.toInt() < 1) "1" else speed.toInt().toString())
                            updateUI()
                        }
                }*/
            }
        })

        binding.fanSwitch.setOnCheckedChangeListener { _, isChecked ->
            disableUI()
                if (!checkWifiIsRunning) {
                    checkWifiIsRunning = true
                    toggleWifi.postDelayed(wifiRunnable, 10000)
                }
            if (!isChecked) {
                updateLive(ZERO, FAN)
            } else {
                var oldFanSpeed = sharedPref!!.getString("old_fan_speed_$currentDeviceId)", "1")
                updateLive(oldFanSpeed.toString(), FAN)

                /*currentDeviceId?.let { device ->
                    devicesDBRef.child(device).child("oldFanSpeed").get()
                        .addOnSuccessListener { speed ->
                            oldFanSpeed = speed.value.toString()
                            devicesDBRef.child(device).child(FAN).setValue(oldFanSpeed)
                                .addOnSuccessListener {
                                    updateUI()
                                }
                        }.addOnFailureListener {
                            devicesDBRef.child(device).child(FAN).setValue(oldFanSpeed)
                                .addOnSuccessListener {
                                    updateUI()
                                }
                        }
                }*/
            }
        }

        binding.switch1Switch.setOnCheckedChangeListener { _, isChecked ->
            disableUI()
                if (!checkWifiIsRunning) {
                    checkWifiIsRunning = true
                    toggleWifi.postDelayed(wifiRunnable, 10000)
                }
            updateLive(if (isChecked) ONE else ZERO, APPL1)

            /*currentDeviceId?.let {
                devicesDBRef.child(it).child(APPL1).setValue(if (isChecked) ONE else ZERO)
                    .addOnSuccessListener {
                        updateUI()
                    }
            }*/
        }

        binding.switch2Switch.setOnCheckedChangeListener { _, isChecked ->
            disableUI()
                if (!checkWifiIsRunning) {
                    checkWifiIsRunning = true
                    toggleWifi.postDelayed(wifiRunnable, 10000)
                }
            updateLive(if (isChecked) ONE else ZERO, APPL2)

            /*currentDeviceId?.let {
                devicesDBRef.child(it).child(APPL2).setValue(if (isChecked) ONE else ZERO)
                    .addOnSuccessListener {
                        updateUI()
                    }
            }*/
        }

        binding.switch3Switch.setOnCheckedChangeListener { _, isChecked ->
            disableUI()
                if (!checkWifiIsRunning) {
                    checkWifiIsRunning = true
                    toggleWifi.postDelayed(wifiRunnable, 10000)
                }
            updateLive(if (isChecked) ONE else ZERO, APPL3)

            /*currentDeviceId?.let {
                devicesDBRef.child(it).child(APPL3).setValue(if (isChecked) ONE else ZERO)
                    .addOnSuccessListener {
                        updateUI()
                    }
            }*/
        }

        binding.switch4Switch.setOnCheckedChangeListener { _, isChecked ->
            disableUI()
                if (!checkWifiIsRunning) {
                    checkWifiIsRunning = true
                    toggleWifi.postDelayed(wifiRunnable, 10000)
                }
            updateLive(if (isChecked) ONE else ZERO, APPL4)

            /*currentDeviceId?.let {
                devicesDBRef.child(it).child(APPL4).setValue(if (isChecked) ONE else ZERO)
                    .addOnSuccessListener {
                        updateUI()
                    }
            }*/
        }

        /*binding.switch1MoreBtn.setOnClickListener {
            showPopupMenu(it, SWITCH1)
        }
        binding.switch2MoreBtn.setOnClickListener {
            showPopupMenu(it, SWITCH2)
        }
        binding.switch3MoreBtn.setOnClickListener {
            showPopupMenu(it, SWITCH3)
        }
        binding.switch4MoreBtn.setOnClickListener {
            showPopupMenu(it, SWITCH4)
        }*/
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
                        // TODO: Show snackbar to retry
//                        showToast("unable to get data")
//                        showErrorScreen()
                        Log.e(TAG, "updateLive: Message - $msg")
                    }
                } catch (e: Exception) {
                    loadingDialog.dismiss()
                    Log.e(TAG, "Exception in updateLive: $e")
                    showToast(e.message)
                }
            }, {
                loadingDialog.dismiss()
                showToast("Something went wrong.")
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
        checkWifi = true }

    private fun togglePower(app1Val: String, app2Val: String, app3Val: String, app4Val: String,
        fan: String) {
        if (isAdded) {
            try {
                if (app1Val == ZERO && app2Val == ZERO && app3Val == ZERO && app4Val == ZERO && fan == ZERO) {
//                    profileDBRef.child("devices").child(currentDeviceId.toString()).child("power")
//                        .setValue(0)
                    binding.powerBtn.setImageDrawable(
                        context?.let { ContextCompat.getDrawable(it, R.drawable.ic_power_btn_off) })
                    enableUI()
                    loadingDialog.dismiss()
                } else {
//                    profileDBRef.child("devices").child(currentDeviceId.toString()).child("power")
//                        .setValue(1)
                    binding.powerBtn.setImageDrawable(
                        context?.let { ContextCompat.getDrawable(it, R.drawable.ic_power_btn_on) })
                    enableUI()
                    loadingDialog.dismiss()
                }
            } catch (e: Exception) {
                Log.e(TAG, "togglePower: Error", e)
            }
        } else togglePower()
    }

    private fun togglePower() {
        /*profileDBRef.child(DEVICES).child(currentDeviceId!!).child(POWER).get()
            .addOnSuccessListener {
                val power = it.value.toString()

                devicesDBRef.child(currentDeviceId.toString()).child(APPL1)
                    .setValue(if (power == ONE) ZERO else ONE)
                    .addOnSuccessListener {
                        devicesDBRef.child(currentDeviceId.toString()).child(APPL2)
                            .setValue(if (power == ONE) ZERO else ONE)
                            .addOnSuccessListener {
                                devicesDBRef.child(currentDeviceId.toString()).child(APPL3)
                                    .setValue(if (power == ONE) ZERO else ONE)
                                    .addOnSuccessListener {
                                        devicesDBRef.child(currentDeviceId.toString())
                                            .child(APPL4)
                                            .setValue(if (power == ONE) ZERO else ONE)
                                            .addOnSuccessListener {
                                                devicesDBRef.child(currentDeviceId.toString())
                                                    .child("oldFanSpeed").get()
                                                    .addOnSuccessListener { speed ->
                                                        devicesDBRef.child(currentDeviceId!!)
                                                            .child(FAN)
                                                            .setValue(
                                                                if (power == ONE) ZERO else speed.value.toString())
                                                            .addOnSuccessListener {
                                                                updateUI()
                                                                loadingDialog.dismiss()
                                                            }.addOnFailureListener { togglePower() }
                                                    }.addOnFailureListener {
                                                        devicesDBRef.child(
                                                            currentDeviceId.toString())
                                                            .child(FAN)
                                                            .setValue(
                                                                if (power == ONE) ZERO else ONE)
                                                            .addOnSuccessListener {
                                                                updateUI()
                                                                loadingDialog.dismiss()
                                                            }.addOnFailureListener { togglePower() }
                                                    }.addOnFailureListener { togglePower() }
                                            }.addOnFailureListener { togglePower() }
                                    }.addOnFailureListener { togglePower() }
                            }.addOnFailureListener { togglePower() }
                    }.addOnFailureListener { togglePower() }
            }*/
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

    private fun showPopupMenu(view: View?, switchID: String) {
        val popup = PopupMenu(requireActivity(), view)
        popup.menuInflater.inflate(R.menu.switch_menu, popup.menu)
        popup.setOnMenuItemClickListener {
            when (it!!.itemId) {
                R.id.edit -> {
                    val intent = Intent(context, EditSwitchActivity::class.java)
                    intent.putExtra(ROOM_ID, currentDeviceId)
                    intent.putExtra(SWITCH_ID, switchID)
                    startActivity(intent)
                }
            }
            true
        }
        popup.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {}

    private fun disableUI() {
        if (!isOnline()) showLSnackbar("No Internet Connection")
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
        } else showLSnackbar("No Internet Connection")
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

    private fun showToast(message: String?) {
        Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show()
    }

    private fun showSSnackbar(msg: String = "Please wait...") {
        Snackbar.make(binding.rcRootView, msg, Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.rcRootView)
//            .setAction("Retry") {
//
//            }
            .show()
    }

    private fun showLSnackbar(msg: String = "Something went wrong.") {
        if (context != null) {
//            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            Snackbar.make(binding.rcRootView, msg, Snackbar.LENGTH_INDEFINITE)
                .setAction("Retry") {
                    if (isOnline()) refreshUI()
                    else showLSnackbar(msg)
                }
                .show()
        } else {
            Log.e(TAG, "showLSnackbar: Contect Error - $context")
            checkDatabase()
        }
    }

    private fun refreshUI() {
        loadingDialog = LoadingDialog()
        loadingDialog.isCancelable = false

        if (!currentDeviceId.isNullOrBlank() || !currentDeviceId.equals("null")) checkDatabase()
        else {
//            loadingDialog.isCancelable = true
//            loadingDialog.dismiss()
            Log.i(TAG, "onViewCreated: ~~~~ $currentDeviceId is not present")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume: Called $currentDeviceId")
        refreshUI()
    }
}