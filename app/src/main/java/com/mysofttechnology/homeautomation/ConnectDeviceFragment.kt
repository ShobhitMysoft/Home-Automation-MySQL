package com.mysofttechnology.homeautomation

import android.app.AlertDialog
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.mysofttechnology.homeautomation.databinding.FragmentConnectDeviceBinding
import java.io.IOException
import java.net.SocketException
import java.util.*
import kotlin.collections.ArrayList


private const val TAG = "ConnectDeviceFragment"

class ConnectDeviceFragment : Fragment() {

    private var _binding: FragmentConnectDeviceBinding? = null
    private val bind get() = _binding!!

    private var bluetoothAdapter: BluetoothAdapter? = null

    private var deviceNameList: ArrayList<String> = arrayListOf()
    private var btDeviceList: ArrayList<BluetoothDevice> = arrayListOf()
    private lateinit var listAdapter: ArrayAdapter<String>

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
                .navigate(R.id.action_connectDeviceFragment_to_scanDeviceFragment)
        }

        bind.devicesLv.setOnItemClickListener { _, _, position, _ ->
            Log.d(
                TAG,
                "Device Name = ${deviceNameList[position]}\nBT Device = ${btDeviceList[position]}"
            )

            val action =
                ConnectDeviceFragmentDirections.actionConnectDeviceFragmentToFillWifiDetailFragment(
                    btDeviceList[position].toString()
                )
            findNavController().navigate(action)

        }
    }

    private fun loadPairedDevices() {
        bind.scanningTv.visibility = View.VISIBLE
        bind.refreshFab.visibility = View.GONE

        val bluetoothManager =
            activity?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        deviceNameList.clear()
        if (bluetoothAdapter?.isEnabled == false) {
            showDialog()
        } else {

            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            pairedDevices?.forEach { device ->
                val deviceName = device.name
//                val deviceHardwareAddress = device.address // MAC address

                if (!deviceName.isNullOrBlank() && deviceName.take(4) == "HOME") {
                    deviceNameList.add(deviceName.toString())
                    btDeviceList.add(device)
                    listAdapter.notifyDataSetChanged()
                }

                Log.d(TAG, "loadPairedDevices: $deviceName")
            }
            bind.scanningTv.visibility = View.GONE
            bind.refreshFab.visibility = View.VISIBLE
            bind.devicesLv.adapter = listAdapter
        }
    }

    private fun showDialog() {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Bluetooth not available")
            .setMessage("Please turn on the bluetooth in order to connect to a home automation device.")
            .setPositiveButton(
                "Try again"
            ) { _, _ ->
                loadPairedDevices()
            }
            .setNeutralButton("Go back") { _, _ ->
                // TODO: Handle LOOP
                Navigation.findNavController(requireView())
                    .navigate(R.id.action_connectDeviceFragment_to_scanDeviceFragment)
            }
        // Create the AlertDialog object and return it
        builder.create()
        builder.show()
    }
}