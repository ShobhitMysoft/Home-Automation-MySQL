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
import com.android.volley.toolbox.StringRequest
import com.google.android.material.snackbar.Snackbar
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

    private var s1Name: String = "Switch 1"
    private var s2Name: String = "Switch 2"
    private var s3Name: String = "Switch 3"
    private var s4Name: String = "Switch 4"
    private var s1Icon: Int = 0
    private var s2Icon: Int = 0
    private var s3Icon: Int = 0
    private var s4Icon: Int = 0

    private lateinit var deviceViewModel: DeviceViewModel
    private var sharedPref: SharedPreferences? = null
    private lateinit var auth: FirebaseAuth

    private var _binding: FragmentDashbordBinding? = null
    private val binding get() = _binding!!

    private var currentUser: FirebaseUser? = null
    private var cuPhoneNo: String? = null
    private var currentUserId: String? = null

    private lateinit var loadingDialog: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadingDialog = LoadingDialog()
        loadingDialog.isCancelable = false
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

        sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE) ?: return
        currentUserId = sharedPref?.getString(getString(R.string.current_user_id), "")
//        binding.actionbarTv.text = currentUserId

        loadingDialog.show(childFragmentManager, TAG)

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
                            loadingDialog.dismiss()
                            updateUI(false, mData)
//                            showToast("No user found. Please register first.")
                            Log.d(TAG, "checkDeviceAvailability: Message - $msg")
                        }
                    } catch (e: Exception) {
                        loadingDialog.dismiss()
                        Log.d(TAG, "Exception in checkDeviceAvailability: $e")
                        showToast(e.message)
                    }
                }, {
                    loadingDialog.dismiss()
                    showToast("Something went wrong.")
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
            loadingDialog.dismiss()
            Snackbar.make(binding.dashRootView, "No internet.", Snackbar.LENGTH_INDEFINITE)
                .setAction("Retry") {
                    checkDeviceAvailability()
                }.show()

            viewLocalDb()
        }
    }

    private fun viewLocalDb() {
        val x = ViewModelProvider(this).get(DeviceViewModel::class.java)
        x.readAllData.observe(viewLifecycleOwner) { device ->
            device.forEach {
                Log.d(TAG, "loadOfflineDb: ${it.name}")
            }
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

            loadingDialog.dismiss()
            binding.addDeviceBtn.visibility = View.GONE
            binding.fragmentContainerView2.findNavController().navigate(R.id.roomControlsFragment)
        } else {
            Log.d(TAG, "updateUI: No device available")
            checkLocalDatabase()
            loadingDialog.dismiss()
            binding.addDeviceBtn.visibility = View.VISIBLE
            binding.fragmentContainerView2.findNavController().navigate(R.id.addDeviceFragment)
        }
    }

    private fun createLocalDatabase(
        deviceListData: JSONArray) {                                                                // TODO: Step 3.1
        val requestQueue = VolleySingleton.getInstance(requireContext()).requestQueue
        val switchListUrl = getString(R.string.base_url) + getString(R.string.url_switch_list)

        for (i in 0 until deviceListData.length()) {
            val deviceData = deviceListData.getJSONObject(i)
            val deviceId = deviceData.get("device_id").toString()
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
                            for (i in 0..4) {
                                val switchData = switchListData.getJSONObject(i)
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

                            val device =
                                Device(0, "Room $deviceId", deviceId, bluetoothId, s1Name, s1Icon, 0, s2Name, s2Icon, 0,
                                    s3Name, s3Icon, 0, s4Name, s4Icon, 0, 0, 0)
                            deviceViewModel.addDevice(device)
                            Log.d(TAG, "createLocalDB: Created!")

                            Log.d(TAG, "switchList: Message - $msg")
                        } else {
                            loadingDialog.dismiss()
                            // TODO: Failed to get room data
//                            showPSnackbar("Failed to get room data")
                            Log.e(TAG, "switch switchList: Message - $msg")
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

            requestQueue.add(switchListRequest)
        }

        viewLocalDb()
        checkLocalDatabase()
    }

    private fun checkLocalDatabase() {
        val deviceData = ViewModelProvider(this).get(DeviceViewModel::class.java)
        deviceData.readAllData.observe(viewLifecycleOwner) { device ->
            device.forEach {
                Log.d(TAG, "checkLocalDatabase: ${it.name}")
            }
        }
    }

    private fun showToast(message: String?) {
        Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show()
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
                    findNavController().navigate(action)
                }
                R.id.logout -> {
                    builder.setTitle("Logout").setMessage("Are you sure you want to logout?")
                        .setPositiveButton("Yes"
                        ) { _, _ ->
                            loadingDialog.show(childFragmentManager, TAG)
                            signOutUser()
                            val action =
                                DashbordFragmentDirections.actionDashbordFragmentToRegistrationFragment()
                            findNavController().navigate(action)
                            loadingDialog.dismiss()
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
}