package com.mysofttechnology.homeautomation

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.intuit.sdp.BuildConfig
import com.mysofttechnology.homeautomation.StartActivity.Companion.ICON
import com.mysofttechnology.homeautomation.StartActivity.Companion.SWITCH
import com.mysofttechnology.homeautomation.database.Device
import com.mysofttechnology.homeautomation.databinding.FragmentDashbordBinding
import com.mysofttechnology.homeautomation.models.DeviceViewModel
import com.mysofttechnology.homeautomation.utils.VolleySingleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.json.JSONArray
import org.json.JSONObject
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import kotlin.collections.set

private const val TAG = "DashbordFragment"

class DashbordFragment : Fragment() {

    private val packageName: String = "com.mysofttechnology.homeautomation"
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

    private var backPressedTime: Long = 0

    private lateinit var deviceViewModel: DeviceViewModel
    private lateinit var requestQueue: RequestQueue
    private var sharedPref: SharedPreferences? = null
    private lateinit var auth: FirebaseAuth

    private var _binding: FragmentDashbordBinding? = null
    private val binding get() = _binding!!

    private var currentUser: FirebaseUser? = null
    private var cuPhoneNo: String? = null
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val backToast = Toast.makeText(requireActivity(), "Press back again to exit the app.",
            Toast.LENGTH_LONG)

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (backPressedTime + 2000 > System.currentTimeMillis()) {
                backToast.cancel()
                activity?.finish()
            } else {
                backToast.show()
            }
            backPressedTime = System.currentTimeMillis()
        }

        callback.isEnabled = true
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

        binding.moreMenu.setOnClickListener {
            showPopupMenu(it)
        }

        binding.addDeviceBtn.setOnClickListener {
            Navigation.findNavController(it)
                .navigate(R.id.action_dashbordFragment_to_scanQrAnimationFragment)
        }

        binding.updateBtn.setOnClickListener {
            goToPlayStore()
        }
    }

    private fun goToPlayStore() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
//        binding.newUpdateView.visibility = View.GONE
    }

    private fun checkDeviceAvailability() {
        if (isOnline()) {
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
        mData: JSONObject) {
        deviceViewModel.clearDatabase()
        if (flag) {
            val deviceListData = mData.get("data") as JSONArray
            createLocalDatabase(deviceListData)
        } else {
            Log.d(TAG, "updateUI: No device available")

            binding.addDeviceBtn.visibility = View.VISIBLE
            binding.noDeviceMsg.visibility = View.VISIBLE
            binding.fragmentContainerView2.findNavController().navigate(R.id.addDeviceFragment)
        }
        // TODO:
//        checkForNewUpdate()
    }

    // TODO: Not completed yet!
    private fun checkForNewUpdate() {
        if (isOnline()) {
            val requestQueue = VolleySingleton.getInstance(requireContext()).requestQueue
            val newUpdateUrl = getString(R.string.base_url) + "getString(R.string.url_new_update)"

            val stringRequest = object : StringRequest(Method.POST, newUpdateUrl,
                { response ->
                    try {
                        val mData = JSONObject(response.toString())
                        val resp = mData.get("response") as Int
                        val msg = mData.get("msg")

                        if (resp == 1) {
                            val currentVersion = mData.get("current_version") as String
                            if (BuildConfig.VERSION_CODE < currentVersion.toInt()) {
                                binding.newUpdateView.visibility = View.VISIBLE
                            }
                        } else {
                            binding.newUpdateView.visibility = View.GONE
                        }
                    } catch (e: Exception) {

                        Log.d(TAG, "Exception in checkForNewUpdate: $e")
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
        }
    }

    private fun createLocalDatabase(
        deviceListData: JSONArray) {
        requestQueue = VolleySingleton.getInstance(requireContext()).requestQueue
        val switchListUrl = getString(R.string.base_url) + getString(R.string.url_switch_list)

        devicesLen = deviceListData.length()

        for (i in 0 until devicesLen) {
            Log.d(TAG, "createLocalDatabase: $deviceListData")
            val deviceData = deviceListData.getJSONObject(i)
            val roomName = deviceData.get("room_name").toString()
            val deviceId = deviceData.get("device_id").toString()
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

    private fun getLiveStates(roomName: String, deviceId: String, bluetoothId: String,
        switchCount: String = "5") {

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

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun checkLocalDatabase() {
        val allData = deviceViewModel.readAllData

        allData.invokeOnCompletion { cause ->
            if (cause != null) {
                Log.i(TAG, "checkLocalDatabase: $cause")
            } else {
                val mData = allData.getCompleted()

                Log.d(TAG, "checkLocalDatabase: $mData")

                try {
                    if (mData.isNotEmpty()) {

                        binding.addDeviceBtn.visibility = View.GONE
                        binding.noDeviceMsg.visibility = View.GONE
                        binding.fragmentContainerView2.findNavController()
                            .navigate(R.id.roomControlsFragment)
                    } else {

                        binding.addDeviceBtn.visibility = View.VISIBLE
                        binding.noDeviceMsg.visibility = View.VISIBLE
                        binding.fragmentContainerView2.findNavController()
                            .navigate(R.id.addDeviceFragment)
                    }
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "checkLocalDatabase: Error", e)
                }
            }
        }
        /*allData.observe(viewLifecycleOwner) {
            try {
                if (it.isNotEmpty()) {

                    binding.addDeviceBtn.visibility = View.GONE
                    binding.noDeviceMsg.visibility = View.GONE
                    binding.fragmentContainerView2.findNavController()
                        .navigate(R.id.roomControlsFragment)
                } else {

                    binding.addDeviceBtn.visibility = View.VISIBLE
                    binding.noDeviceMsg.visibility = View.VISIBLE
                    binding.fragmentContainerView2.findNavController()
                        .navigate(R.id.addDeviceFragment)
                }
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "checkLocalDatabase: Error", e)
            }
        }*/
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
                    if (findNavController().currentDestination?.id == R.id.dashbordFragment)
                        findNavController().navigate(action)
                }
                R.id.rooms -> {
                    val action = DashbordFragmentDirections.actionDashbordFragmentToRoomsFragment()
                    if (isOnline()) {
                        if (findNavController().currentDestination?.id == R.id.dashbordFragment)
                            findNavController().navigate(action)
                    } else showToast("No internet connection")
                }
                R.id.logout -> {
                    builder.setTitle("Logout").setMessage("Are you sure you want to logout?")
                        .setPositiveButton("Yes"
                        ) { _, _ ->
                            signOutUser()
                            val action =
                                DashbordFragmentDirections.actionDashbordFragmentToRegistrationFragment()
                            if (findNavController().currentDestination?.id == R.id.dashbordFragment)
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

    override fun onResume() {
        super.onResume()

        checkDeviceAvailability()

        /*val isDashboardFabPromptShown =
            sharedPref?.getBoolean(getString(R.string.isDashboardFabPromptShown), false)
        if (isDashboardFabPromptShown == false) checkDeviceAvailability()
        else showFabPrompt()*/
    }

    /*private fun showFabPrompt() {
        MaterialTapTargetPrompt.Builder(requireActivity())
            .setTarget(binding.moreMenu)
            .setBackgroundColour(Color.DKGRAY)
//            .setPrimaryText("NEXT")
            .setSecondaryText(
                "More options to refresh the data, view profile, view all of your rooms and logout from your account.")
            .setBackButtonDismissEnabled(false)
            .setPromptStateChangeListener { _, state ->
                if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED || state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                    sharedPref?.edit()?.putBoolean(getString(R.string.isDashboardFabPromptShown), true)
                        ?.apply()
                    checkDeviceAvailability()
                }
            }
            .show()
    }*/

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