package com.mysofttechnology.homeautomation

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.android.volley.toolbox.StringRequest
import com.budiyev.android.codescanner.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.mysofttechnology.homeautomation.StartActivity.Companion.BLANK
import com.mysofttechnology.homeautomation.StartActivity.Companion.FRI
import com.mysofttechnology.homeautomation.StartActivity.Companion.START_TIME
import com.mysofttechnology.homeautomation.StartActivity.Companion.STOP_TIME
import com.mysofttechnology.homeautomation.StartActivity.Companion.MON
import com.mysofttechnology.homeautomation.StartActivity.Companion.SAT
import com.mysofttechnology.homeautomation.StartActivity.Companion.SUN
import com.mysofttechnology.homeautomation.StartActivity.Companion.THU
import com.mysofttechnology.homeautomation.StartActivity.Companion.TUE
import com.mysofttechnology.homeautomation.StartActivity.Companion.WED
import com.mysofttechnology.homeautomation.StartActivity.Companion.ZERO
import com.mysofttechnology.homeautomation.databinding.FragmentScanDeviceBinding
import com.mysofttechnology.homeautomation.utils.VolleySingleton
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "ScanDeviceFragment"

class ScanDeviceFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    private var currentUser: FirebaseUser? = null
    private var cuPhoneNo: String? = null

    private var _binding: FragmentScanDeviceBinding? = null
    private val binding get() = _binding!!

    private lateinit var codeScanner: CodeScanner
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

        auth = FirebaseAuth.getInstance()

        currentUser = auth.currentUser
        cuPhoneNo = currentUser?.phoneNumber.toString().takeLast(10)

        _binding = FragmentScanDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activityResultLauncher.launch(Manifest.permission.CAMERA)
        codeScanner()

        binding.backBtn.setOnClickListener {
            binding.backBtn.isEnabled = false
            Navigation.findNavController(it)
                .navigate(R.id.action_scanDeviceFragment_to_roomsFragment)
        }

        binding.sdContinueBtn.setOnClickListener {
            binding.sdContinueBtn.isEnabled = false
            val deviceId = binding.deviceIdEt.text.toString()
            if (deviceId.isNotEmpty()) {
                loadingDialog.show(childFragmentManager, TAG)
                checkDeviceAvailability(deviceId)
            } else {
                Toast.makeText(
                    requireActivity(),
                    "Enter a valid device id",
                    Toast.LENGTH_SHORT
                ).show()
                binding.sdContinueBtn.isEnabled = true
            }
        }
    }

    private fun codeScanner() {
//        val scannerView = bind.barcodeScanner
        codeScanner = CodeScanner(requireActivity(), binding.barcodeScanner)

        codeScanner.apply {
            camera = CodeScanner.CAMERA_BACK
            formats =
                CodeScanner.ALL_FORMATS       // Or can specify for other formats like 1-D and 2-D

            autoFocusMode = AutoFocusMode.SAFE
            scanMode =
                ScanMode.SINGLE          // The scanner will keen scanning continuously by itself and If set to "SINGLE" you will have to keep pressing to scan the code
            isAutoFocusEnabled = false
            isFlashEnabled =
                false                  // Flash is disabled in start, user will click to start it

            // RESPONSE if worked
            decodeCallback = DecodeCallback {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireActivity(), "$it", Toast.LENGTH_SHORT).show()
                    binding.deviceIdEt.setText(it.toString())
                    loadingDialog.show(childFragmentManager, TAG)
                    checkDeviceAvailability(it.toString())
                }
            }

            // Error Response
            errorCallback = ErrorCallback {
                requireActivity().runOnUiThread {
                    Log.d(TAG, "Camera Initialisation Error: ${it.message}")
                }
            }

            // To tell the program to start scanning the QR Code
            binding.barcodeScanner.setOnClickListener {
                codeScanner.startPreview()
            }
        }
    }

    private fun checkDeviceAvailability(deviceId: String) {
        val requestQueue = VolleySingleton.getInstance(requireContext()).requestQueue
        val url = getString(R.string.base_url)+getString(R.string.url_room_list)

        val stringRequest = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val mData = JSONObject(response.toString())
                    val resp = mData.get("response") as Int
                    val msg = mData.get("msg")

                    if (resp == 1) {
                        val roomListData = mData.get("data") as JSONArray
                        if (roomExists(roomListData, deviceId)) {
                            // TODO: Room Already exists dialog(Choose continue to add wifi or finish)
                            showToast("Device already exists")
                            gotoConnectDevice()
                        } else {
                            showToast("New device")
                            addDevice(deviceId)
                        }
                        Log.d(TAG, "checkDeviceAvailability: Message - $msg")
                    } else {
                        loadingDialog.dismiss()
                        showToast("New device")
                        addDevice(deviceId)
                        Log.d(TAG, "checkDeviceAvailability: Message - $msg")
                    }
                } catch (e: Exception) {
                    loadingDialog.dismiss()
                    Log.e(TAG, "Exception: $e")
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
    }

    private fun roomExists(roomListData: JSONArray, deviceId: String): Boolean {
        var flag = false
        for (i in 0 until roomListData.length()) {
            val room = roomListData.getJSONObject(i)
            if (room.get("device_id") == deviceId) {
                flag = true
            }
        }
        return flag
    }

    private fun addDevice(deviceId: String) {
        val requestQueue = VolleySingleton.getInstance(requireContext()).requestQueue
        val url = getString(R.string.base_url)+getString(R.string.url_room)

        val stringRequest = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val mData = JSONObject(response.toString())
                    val resp = mData.get("response") as Int
                    val msg = mData.get("msg")

                    if (resp == 1) {
                        for (i in 1..5) {
                            createSwitch(deviceId, i)
                        }
                        Log.d(TAG, "addDevice: Message - $msg")
                    } else {
                        loadingDialog.dismiss()
                        showToast("unable to create room")
                        Log.e(TAG, "addDevice: Message - $msg")
                    }
                } catch (e: Exception) {
                    loadingDialog.dismiss()
                    Log.e(TAG, "Exception: $e")
                    showToast(e.message)
                }
            }, {
                loadingDialog.dismiss()
                showToast("Something went wrong.")
                Log.e(TAG, "VollyError: ${it.message}")
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["device_id"] = deviceId
                params["user_id"] = cuPhoneNo.toString()
                params["room_name"] = "Room $deviceId"
                params["power"] = ZERO
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

    private fun createSwitch(deviceId: String, i: Int) {
        val requestQueue = VolleySingleton.getInstance(requireContext()).requestQueue
        val url = getString(R.string.base_url)+getString(R.string.url_switch)

        val stringRequest = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val mData = JSONObject(response.toString())
                    val resp = mData.get("response") as Int
                    val msg = mData.get("msg")

                    if (resp == 1) {
                        if (i>=5) gotoConnectDevice()
                        Log.d(TAG, "createSwitch: Message - $msg")
                    } else {
                        loadingDialog.dismiss()
                        showToast("Failed to get all data")
                        Log.e(TAG, "createSwitch: Message - $msg")
                    }
                } catch (e: Exception) {
                    loadingDialog.dismiss()
                    Log.e(TAG, "Exception: $e")
                    showToast(e.message)
                }
            }, {
                loadingDialog.dismiss()
                showToast("Something went wrong.")
                Log.e(TAG, "VollyError: ${it.message}")
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["device_id"] = deviceId
                params["switch"] = if (i==5) "Fan" else "Switch $i"
                params["icon"] = ZERO
                params[START_TIME] = BLANK
                params[STOP_TIME] = BLANK
                params[SUN] = ZERO
                params[MON] = ZERO
                params[TUE] = ZERO
                params[WED] = ZERO
                params[THU] = ZERO
                params[FRI] = ZERO
                params[SAT] = ZERO
                params["switch_id_by_app"] = i.toString()
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

    private fun gotoConnectDevice() {
        Navigation.findNavController(requireView())
            .navigate(R.id.action_scanDeviceFragment_to_connectDeviceFragment)
        loadingDialog.dismiss()
    }

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Handle Permission granted/rejected
        if (isGranted) {
            binding.barcodeScanner.visibility = View.VISIBLE
        } else {
            Toast.makeText(requireActivity(), "Camera permission not granted!", Toast.LENGTH_SHORT)
                .show()
            binding.barcodeScanner.visibility = View.GONE
            // TODO: Navigate Up
//                Navigation.findNavController(requireView()).navigateUp()
        }
    }

    private fun showToast(message: String?) {
        Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()

        codeScanner.startPreview()
    }
}