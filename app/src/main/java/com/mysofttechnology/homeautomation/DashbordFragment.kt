package com.mysofttechnology.homeautomation

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.mysofttechnology.homeautomation.StartActivity.Companion.ICON
import com.mysofttechnology.homeautomation.StartActivity.Companion.SWITCH
import com.mysofttechnology.homeautomation.database.Device
import com.mysofttechnology.homeautomation.databinding.FragmentDashbordBinding
import com.mysofttechnology.homeautomation.models.DeviceViewModel
import com.mysofttechnology.homeautomation.utils.VolleySingleton
import org.json.JSONArray
import org.json.JSONObject
import kotlin.collections.set

private const val TAG = "DashbordFragment"

class DashbordFragment : Fragment() {

    private var devicesLen: Int = 0
    private var s1Name: String = "Switch 1"
    private var s2Name: String = "Switch 2"
    private var s3Name: String = "Switch 3"
    private var s4Name: String = "Switch 4"
    private var s6Name: String = "Switch"
    private var s1State: Int = 0
    private var s2State: Int = 0
    private var s3State: Int = 0
    private var s4State: Int = 0
    private var s6State: Int = 0
    private var fan: Int = 0
    private var s1Icon: Int = 0
    private var s2Icon: Int = 0
    private var s3Icon: Int = 0
    private var s4Icon: Int = 0
    private var s6Icon: Int = 0

    private lateinit var deviceViewModel: DeviceViewModel
    private lateinit var requestQueue: RequestQueue
    private var sharedPref: SharedPreferences? = null
    private lateinit var auth: FirebaseAuth

    private var _binding: FragmentDashbordBinding? = null
    private val binding get() = _binding!!

    private var currentUser: FirebaseUser? = null
    private var cuPhoneNo: String? = null
    private var currentUserId: String? = null

//    private lateinit var loadingDialog: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        loadingDialog = LoadingDialog()
//        loadingDialog.isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDashbordBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()

        currentUser = auth.currentUser
        cuPhoneNo = currentUser?.phoneNumber.toString().takeLast(10)

        deviceViewModel = ViewModelProvider(this).get(DeviceViewModel::class.java)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestQueue = VolleySingleton.getInstance(requireContext()).requestQueue
        sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE) ?: return
        currentUserId = sharedPref?.getString(getString(R.string.current_user_id), "")
//        binding.actionbarTv.text = currentUserId

//        loadingDialog.show(childFragmentManager, TAG)

        checkDeviceAvailability()

        binding.moreMenu.setOnClickListener {
            showPopupMenu(it)
        }

        binding.addDeviceBtn.setOnClickListener {
            Navigation.findNavController(it)
                .navigate(R.id.action_dashbordFragment_to_scanDeviceFragment)
        }
    }

    private fun checkDeviceAvailability() {                                                         // TODO: Step 1
        if (isOnline()) {                                                                           // TODO: Step 2
            val requestQueue = VolleySingleton.getInstance(requireContext()).requestQueue
            val url = getString(R.string.base_url) + getString(R.string.url_room_list)

            val stringRequest = object : StringRequest(Method.POST, url,
                { response ->
                    try {
                        val mData = JSONObject(response.toString())
                        val resp = mData.get("response") as Int
                        val msg = mData.get("msg")

                        if (resp == 1) {
                            updateUI(true, mData)
                            Log.d(TAG, "checkDeviceAvailability: Message - $msg")
                        } else {

                            updateUI(false, mData)
                            Log.d(TAG, "checkDeviceAvailability: Message - $msg")
                        }
                    } catch (e: Exception) {

                        Log.d(TAG, "Exception in checkDeviceAvailability: $e")
                        if (isAdded && e.message != null) showToast(e.message)
                    }
                }, {

                    if (isAdded) showToast("Something went wrong.")
                    Log.e(TAG, "VollyError: ${it.message}")
                }) {
                override fun getParams(): Map<String, String> {
                    val params = HashMap<String, String>()
                    params["user_id"] = cuPhoneNo.toString()
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
            checkLocalDatabase()            // Not Online
        }
    }

    private fun updateUI(flag: Boolean,
        mData: JSONObject) {                                                                        // TODO: Step 3
        // TODO: Clear table
        deviceViewModel.clearDatabase()
        if (flag) {
            // TODO: Set local database from online database
            val deviceListData = mData.get("data") as JSONArray
            createLocalDatabase(deviceListData)
        } else {
            Log.d(TAG, "updateUI: No device available")

            binding.addDeviceBtn.visibility = View.VISIBLE
            binding.fragmentContainerView2.findNavController().navigate(R.id.addDeviceFragment)
        }
    }

    private fun createLocalDatabase(
        deviceListData: JSONArray) {                                                                // TODO: Step 3.1
        requestQueue = VolleySingleton.getInstance(requireContext()).requestQueue
        val switchListUrl = getString(R.string.base_url) + getString(R.string.url_switch_list)

        devicesLen = deviceListData.length()

        for (i in 0 until devicesLen) {
            Log.d(TAG, "createLocalDatabase: $deviceListData")
            val deviceData = deviceListData.getJSONObject(i)
            val roomName = deviceData.get("room_name").toString()
            val deviceId = deviceData.get("device_id").toString()
            // TODO// Remove this for single switch device
//            val switchCount = "5"
            val switchCount = deviceData.get("switch_count").toString()
            val bluetoothId = deviceData.get("bluetooth").toString()

            val switchListRequest = object : StringRequest(Method.POST, switchListUrl,
                { response ->
                    Log.i(TAG, "switchList: $response")
                    try {
                        val mData = JSONObject(response.toString())
                        val resp = mData.get("response") as Int
                        val msg = mData.get("msg")

                        if (resp == 1) {
                            val switchListData = mData.get("data") as JSONArray
                            if (switchCount == "1") {
                                val switchData = switchListData.getJSONObject(0)
                                s6Name = switchData.getString(SWITCH)
                                s6Icon = switchData.getString(ICON).toInt()
                            } else {
                                for (j in 0..4) {
                                    val switchData = switchListData.getJSONObject(j)
                                    val switchId = switchData.get("switch_id_by_app").toString()
                                    if (switchId != "5") {
                                        when (switchId) {
                                            "1" -> {
                                                s1Name = switchData.getString(SWITCH)
                                                s1Icon = switchData.getString(ICON).toInt()
                                            }
                                            "2" -> {
                                                s2Name = switchData.getString(SWITCH)
                                                s2Icon = switchData.getString(ICON).toInt()
                                            }
                                            "3" -> {
                                                s3Name = switchData.getString(SWITCH)
                                                s3Icon = switchData.getString(ICON).toInt()
                                            }
                                            else -> {
                                                s4Name = switchData.getString(SWITCH)
                                                s4Icon = switchData.getString(ICON).toInt()
                                            }
                                        }
                                    }
                                }
                            }

                            getLiveStates(roomName, deviceId, bluetoothId, switchCount)

                            Log.d(TAG, "switchList: Message - $msg")
                        } else {

                            // TODO: Failed to get room data
//                            showPSnackbar("Failed to get room data")
                            Log.e(TAG, "switch switchList: Message - $msg")
                        }
                    } catch (e: Exception) {

                        Log.e(TAG, "Exception in switch updateUI: $e")
                        if (isAdded && e.message != null) showToast(e.message)
                    }
                }, {

                    if (isAdded) showToast("Something went wrong.")
                    Log.e(TAG, "VollyError: ${it.message}")
                }) {
                override fun getParams(): Map<String, String> {
                    val params = java.util.HashMap<String, String>()
                    params["device_id"] = deviceId
                    params["mobile_no"] = currentUserId.toString()
                    return params
                }

                override fun getHeaders(): MutableMap<String, String> {
                    val params = java.util.HashMap<String, String>()
                    params["Content-Type"] = "application/x-www-form-urlencoded"
                    return params
                }
            }

            requestQueue.add(switchListRequest)
        }
    }

    private fun getLiveStates(roomName: String, deviceId: String,
        bluetoothId: String,
        switchCount: String = "5") {                              // TODO: Step 3.2

        val getLiveUrl = getString(R.string.base_url) + getString(R.string.url_get_live)

        val liveDataRequest = object : StringRequest(Method.POST, getLiveUrl,
            { response ->
                try {
                    val mData = JSONObject(response.toString())
                    val resp = mData.get("response") as Int
                    val msg = mData.get("msg")

                    if (resp == 1) {
                        if (switchCount == "1") {
                            s6State = (mData.get(StartActivity.APPL1) as String).toInt()
                        } else {
                            s1State = (mData.get(StartActivity.APPL1) as String).toInt()
                            s2State = (mData.get(StartActivity.APPL2) as String).toInt()
                            s3State = (mData.get(StartActivity.APPL3) as String).toInt()
                            s4State = (mData.get(StartActivity.APPL4) as String).toInt()
                            fan = (mData.get(StartActivity.FAN) as String).toInt()
                        }

                        // Creating local Database
                        val device =
                            Device(0, roomName, deviceId, switchCount, bluetoothId, s1Name, s1Icon,
                                s1State, s2Name, s2Icon, s2State, s3Name, s3Icon, s3State, s4Name,
                                s4Icon, s4State, s6Name, s6Icon, s6State, 0, fan)
                        deviceViewModel.addDevice(device)
                        Log.d(TAG, "createLocalDB: Created!")

                        Log.d(TAG, "getLiveStates: Message - $msg")
                    } else {
                        Log.e(TAG, "getLiveStates: Message - $msg")
                    }
                } catch (e: Exception) {

                    Log.e(TAG, "Exception in getLiveStates: $e")
                    if (isAdded && e.message != null) showToast(e.message)
                }
            }, {

                if (isAdded) showToast("Something went wrong.")
                Log.e(TAG, "VollyError: ${it.message}")
            }) {
            override fun getParams(): Map<String, String> {
                val params = java.util.HashMap<String, String>()
                params["device_id"] = deviceId
                return params
            }

            override fun getHeaders(): MutableMap<String, String> {
                val params = java.util.HashMap<String, String>()
                params["Content-Type"] = "application/x-www-form-urlencoded"
                return params
            }
        }

        requestQueue.add(liveDataRequest)
    }

    private fun checkLocalDatabase() {                                                              // TODO: Step 4
        val allData = deviceViewModel.readAllData
        allData.observe(viewLifecycleOwner) {
            try {
                if (it.isNotEmpty()) {

                    binding.addDeviceBtn.visibility = View.GONE
                    binding.fragmentContainerView2.findNavController()
                        .navigate(R.id.roomControlsFragment)
                } else {

                    binding.addDeviceBtn.visibility = View.VISIBLE
                    binding.fragmentContainerView2.findNavController()
                        .navigate(R.id.addDeviceFragment)
                }
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "checkLocalDatabase: Error", e)
            }
        }
    }

    private fun showToast(message: String?) {
        if (isAdded) Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show()
    }

    private fun showPopupMenu(view: View?) {
        val builder = AlertDialog.Builder(requireActivity())

        val popup = PopupMenu(requireActivity(), view)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
        popup.setOnMenuItemClickListener {
            when (it!!.itemId) {
                R.id.refresh -> {
                    checkDeviceAvailability()
                }
                R.id.profile -> {
                    val action =
                        DashbordFragmentDirections.actionDashbordFragmentToProfileFragment()
                    findNavController().navigate(action)
                }
                R.id.rooms -> {
                    val action = DashbordFragmentDirections.actionDashbordFragmentToRoomsFragment()
                    if (isOnline()) findNavController().navigate(action)
                    else showToast("No Internet")
                }
                R.id.logout -> {
                    builder.setTitle("Logout").setMessage("Are you sure you want to logout?")
                        .setPositiveButton("Yes"
                        ) { _, _ ->
//                            loadingDialog.show(childFragmentManager, TAG)
                            signOutUser()
                            val action =
                                DashbordFragmentDirections.actionDashbordFragmentToRegistrationFragment()
                            findNavController().navigate(action)

                        }
                        .setNegativeButton("No") { _, _ -> }
                    builder.create()
                    builder.show()
                }
            }
            true
        }
        popup.show()
    }

    private fun signOutUser() {
        FirebaseAuth.getInstance().signOut()
    }

//    override fun onSaveInstanceState(outState: Bundle) {}

    private fun isOnline(context: Context = requireContext()): Boolean {
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

    override fun onStart() {
        super.onStart()
        val spEditor = sharedPref?.edit()
        spEditor?.putString(getString(R.string.current_user_id), cuPhoneNo)
        spEditor?.apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}