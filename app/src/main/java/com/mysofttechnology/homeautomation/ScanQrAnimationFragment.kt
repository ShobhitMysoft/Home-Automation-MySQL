package com.mysofttechnology.homeautomation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.mysofttechnology.homeautomation.databinding.FragmentScanQrAnimationBinding


private const val TAG = "ScanQrAnimationFragment"

class ScanQrAnimationFragment : Fragment() {

    private var _binding: FragmentScanQrAnimationBinding? = null
    private val bind get() = _binding!!

    private var snackbar: Snackbar? = null
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        _binding = FragmentScanQrAnimationBinding.inflate(inflater, container, false)
        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        snackbar =
            Snackbar.make(bind.sqrRootView, "Permissions are not granted", Snackbar.LENGTH_SHORT)
                .setAction("SETTINGS") {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + BuildConfig.APPLICATION_ID)))
                }
                .setAnchorView(bind.scanQrContinueBtn)


        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all {
                it.value == true
            }

            if (granted) {
                if (findNavController().currentDestination?.id == R.id.scanQrAnimationFragment)
                    findNavController().navigate(
                        R.id.action_scanQrAnimationFragment_to_scanDeviceFragment)
            } else snackbar?.show()
        }

        bind.scanQrContinueBtn.setOnClickListener {
            checkAllPermissions()
        }
    }

    private fun checkAllPermissions() {
        val permReqList: MutableList<String> = arrayListOf()

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        )
            permReqList.add(Manifest.permission.CAMERA)

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        )
            permReqList.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        )
            permReqList.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
            )
                permReqList.add(Manifest.permission.BLUETOOTH_SCAN)

            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
            )
                permReqList.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (permReqList.isNotEmpty()) {
            permissionLauncher.launch(permReqList.toTypedArray())
        } else {
            if (findNavController().currentDestination?.id == R.id.scanQrAnimationFragment)
                findNavController().navigate(
                    R.id.action_scanQrAnimationFragment_to_scanDeviceFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        snackbar?.dismiss()
    }
}