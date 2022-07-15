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

private const val TAG = "ScanDeviceFragment"

class ScanDeviceFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var dbRef: DatabaseReference
    private lateinit var profileDBRef: DatabaseReference

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
        db = FirebaseDatabase.getInstance()
        dbRef = db.getReference("root/devices")

        currentUser = auth.currentUser
        cuPhoneNo = currentUser?.phoneNumber.toString().takeLast(10)

        _binding = FragmentScanDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileDBRef = db.getReference("root/users/$cuPhoneNo/profile")

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
        profileDBRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.child(deviceId).exists()) {
                    Toast.makeText(requireActivity(), "Device already exists", Toast.LENGTH_SHORT)
                        .show()
                    addDevice(deviceId)
                } else {
                    Toast.makeText(requireActivity(), "New device", Toast.LENGTH_SHORT)
                        .show()
                    addDevice(deviceId)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "onCancelled: ${error.message}")
            }
        })
    }

    private fun addDevice(deviceId: String) {
        // TODO: Do not add if already exists (maybe bcoz the user wants to add wifi password so just update)
        // TODO: Add user id to device child(user: UserId) or "connected: 0/1" so that only connected user...
        profileDBRef.get().addOnSuccessListener {
            var deviceCount = (it.child("deviceCount").value as String).toInt()

            if (it.child("devices").hasChild(deviceId)) {
                Navigation.findNavController(requireView())
                    .navigate(R.id.action_scanDeviceFragment_to_connectDeviceFragment)
                loadingDialog.dismiss()
            } else {
                deviceCount++
                profileDBRef.child("deviceCount").setValue(deviceCount.toString())

                profileDBRef.child("devices").child(deviceId).apply {
                    child("name").setValue("Room $deviceCount")
                    child("order").setValue(deviceCount)
                    child("id").setValue(deviceId)
                    child("power").setValue(0)

                    child("switch1").child("icon").setValue(0)
                    child("switch1").child("name").setValue("Switch 1")
                    child("switch1").child(START_TIME).setValue(BLANK)
                    child("switch1").child(STOP_TIME).setValue(BLANK)
                    child("switch1").child(SUN).setValue(ZERO)
                    child("switch1").child(MON).setValue(ZERO)
                    child("switch1").child(TUE).setValue(ZERO)
                    child("switch1").child(WED).setValue(ZERO)
                    child("switch1").child(THU).setValue(ZERO)
                    child("switch1").child(FRI).setValue(ZERO)
                    child("switch1").child(SAT).setValue(ZERO)

                    child("switch2").child("icon").setValue(0)
                    child("switch2").child("name").setValue("Switch 2")
                    child("switch2").child(START_TIME).setValue(BLANK)
                    child("switch2").child(STOP_TIME).setValue(BLANK)
                    child("switch2").child(SUN).setValue(ZERO)
                    child("switch2").child(MON).setValue(ZERO)
                    child("switch2").child(TUE).setValue(ZERO)
                    child("switch2").child(WED).setValue(ZERO)
                    child("switch2").child(THU).setValue(ZERO)
                    child("switch2").child(FRI).setValue(ZERO)
                    child("switch2").child(SAT).setValue(ZERO)

                    child("switch3").child("icon").setValue(0)
                    child("switch3").child("name").setValue("Switch 3")
                    child("switch3").child(START_TIME).setValue(BLANK)
                    child("switch3").child(STOP_TIME).setValue(BLANK)
                    child("switch3").child(SUN).setValue(ZERO)
                    child("switch3").child(MON).setValue(ZERO)
                    child("switch3").child(TUE).setValue(ZERO)
                    child("switch3").child(WED).setValue(ZERO)
                    child("switch3").child(THU).setValue(ZERO)
                    child("switch3").child(FRI).setValue(ZERO)
                    child("switch3").child(SAT).setValue(ZERO)

                    child("switch4").child("icon").setValue(0)
                    child("switch4").child("name").setValue("Switch 4")
                    child("switch4").child(START_TIME).setValue(BLANK)
                    child("switch4").child(STOP_TIME).setValue(BLANK)
                    child("switch4").child(SUN).setValue(ZERO)
                    child("switch4").child(MON).setValue(ZERO)
                    child("switch4").child(TUE).setValue(ZERO)
                    child("switch4").child(WED).setValue(ZERO)
                    child("switch4").child(THU).setValue(ZERO)
                    child("switch4").child(FRI).setValue(ZERO)
                    child("switch4").child(SAT).setValue(ZERO)
                }
                Navigation.findNavController(requireView())
                    .navigate(R.id.action_scanDeviceFragment_to_connectDeviceFragment)
                loadingDialog.dismiss()
            }
        }

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

    override fun onResume() {
        super.onResume()

        codeScanner.startPreview()
    }
}