package com.mysofttechnology.homeautomation

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
            deviceId = it.getString("deviceId").toString()
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

        loadPairedDevices()

        bind.refreshFab.setOnClickListener { loadPairedDevices() }

        bind.backBtn.setOnClickListener {
            bind.backBtn.isEnabled = false
            Navigation.findNavController(it)
                .navigate(R.id.action_connectDeviceFragment_to_roomsFragment)
        }

        bind.devicesLv.setOnItemClickListener { _, _, position, _ ->
            Log.d(
                TAG,
                "Device Name = ${deviceNameList[position]}\nBT Device = ${btDeviceList[position]}"
            )

            gotoFillWifiFrag(btDeviceList[position].toString())

        }
    }

    private fun gotoFillWifiFrag(deviceBtId: String) {
        val action =
            ConnectDeviceFragmentDirections.actionConnectDeviceFragmentToFillWifiDetailFragment(
                deviceBtId, deviceId.toString()
            )
        findNavController().navigate(action)
    }

    private fun loadPairedDevices() {
        bind.scanningTv.visibility = View.VISIBLE
        bind.refreshFab.visibility = View.GONE

        val bluetoothManager =
            activity?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        deviceNameList.clear()
        if (bluetoothAdapter?.isEnabled == false) {
//            showDialog()
            registerForResult.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } else {

            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            pairedDevices?.forEach { device ->
                val deviceName = device.name
                val deviceIdDigits = deviceId?.substring(4, deviceId!!.length)
//                val deviceHardwareAddress = device.address // MAC address

                if (deviceName?.contains("$deviceIdDigits") == true) gotoFillWifiFrag(device.toString())

                if (deviceName.take(2) == "SL") {
                    deviceNameList.add(deviceName.toString())
                    btDeviceList.add(device)
                    listAdapter.notifyDataSetChanged()
                }

                Log.d(TAG, "loadPairedDevices: $deviceName | $device")
            }
            bind.scanningTv.visibility = View.GONE
            bind.refreshFab.visibility = View.VISIBLE
            bind.devicesLv.adapter = listAdapter
        }
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

    private val registerForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadPairedDevices()
        } else Navigation.findNavController(requireView()).navigate(R.id.action_connectDeviceFragment_to_roomsFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}