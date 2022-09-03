package com.mysofttechnology.homeautomation

import android.Manifest
import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.mysofttechnology.homeautomation.databinding.FragmentConnectDeviceBinding


private const val TAG = "ConnectDeviceFragment"

class ConnectDeviceFragment : Fragment() {

    private var deviceId: String? = null
    private var _binding: FragmentConnectDeviceBinding? = null
    private val bind get() = _binding!!

    private var bluetoothAdapter: BluetoothAdapter? = null

    private var deviceNameList: ArrayList<String> = arrayListOf()
    private var btDeviceList: ArrayList<BluetoothDevice> = arrayListOf()
    private lateinit var listAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            deviceId = it.getString("deviceId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "onCreateView: Called")
        _binding = FragmentConnectDeviceBinding.inflate(inflater, container, false)
        listAdapter =
            ArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, deviceNameList)
        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated: Called")

//        loadPairedDevices()
        checkPermissions()

        bind.refreshFab.setOnClickListener { checkPermissions() }

        bind.backBtn.setOnClickListener {
            bind.backBtn.isEnabled = false
            Navigation.findNavController(it)
                .navigate(R.id.action_connectDeviceFragment_to_roomsFragment)
        }

        bind.btSettingsBtn.setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
        }

        bind.devicesLv.setOnItemClickListener { _, _, position, _ ->
            Log.d(
                TAG,
                "Device Name = ${deviceNameList[position]}\nBT Device = ${btDeviceList[position]}"
            )
            gotoFillWifiFrag(btDeviceList[position].toString())
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT))
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        }
    }

    private fun gotoFillWifiFrag(deviceBtId: String) {
        val action =
            ConnectDeviceFragmentDirections.actionConnectDeviceFragmentToFillWifiDetailFragment(
                deviceBtId, deviceId.toString()
            )
        if (findNavController().currentDestination?.id == R.id.connectDeviceFragment)
            findNavController().navigate(action)
        else gotoRoomsFragment()
    }

    private fun gotoRoomsFragment() {
        if (findNavController().currentDestination?.id == R.id.fillWifiDetailFragment)
            Navigation.findNavController(requireView())
                .navigate(R.id.action_fillWifiDetailFragment_to_roomsFragment)
        else if (findNavController().currentDestination?.id == R.id.connectDeviceFragment)
            Navigation.findNavController(requireView())
                .navigate(R.id.action_connectDeviceFragment_to_roomsFragment)
    }

    private fun loadPairedDevices() {
        bind.refreshFab.visibility = View.GONE

        val bluetoothManager =
            activity?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        deviceNameList.clear()
//        if (bluetoothAdapter?.isEnabled == false) {
////            showDialog()
//            checkPermissions()
//        } else {

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            if (deviceNameList.isNotEmpty()) bind.noPairedDeviceView.visibility = View.GONE
            else bind.noPairedDeviceView.visibility = View.VISIBLE

            val deviceName = device.name
            val deviceIdDigits = deviceId?.substring(4, deviceId!!.length)

//                val deviceHardwareAddress = device.address // MAC address

            if (deviceName?.contains(
                    "$deviceIdDigits") == true || deviceName == deviceId
            ) gotoFillWifiFrag(device.toString())

            if (deviceName.take(2) == "SL") {
                deviceNameList.add(deviceName.toString())
                btDeviceList.add(device)
                listAdapter.notifyDataSetChanged()
            }
        }
        bind.refreshFab.visibility = View.VISIBLE
        bind.devicesLv.adapter = listAdapter
//        }
    }

    /*private fun showDialog() {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Bluetooth not available")
            .setMessage("Please turn on the bluetooth in order to connect to a ${
                getString(R.string.app_name)
            } device.")
            .setPositiveButton(
                "Ok"
            ) { _, _ ->

            }
            .setNeutralButton("Go back") { _, _ ->
                Navigation.findNavController(requireView())
                    .navigate(R.id.action_connectDeviceFragment_to_roomsFragment)
            }
        builder.create()
        builder.show()
    }*/

    private var requestBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                loadPairedDevices()
            } else {
                Toast.makeText(requireActivity(), "Permission not granted", Toast.LENGTH_SHORT)
                    .show()
                if (findNavController().currentDestination?.id == R.id.connectDeviceFragment)
                    findNavController().navigate(R.id.action_connectDeviceFragment_to_roomsFragment)
            }
        }

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.entries.all {
            it.value == true
        }

        if (granted) {
            loadPairedDevices()
        } else Toast.makeText(requireActivity(), "Permissions are not granted", Toast.LENGTH_SHORT)
            .show()
    }

    /*private val registerForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadPairedDevices()
        } else Navigation.findNavController(requireView())
            .navigate(R.id.action_connectDeviceFragment_to_roomsFragment)
    }*/

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}