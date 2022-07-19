package com.mysofttechnology.homeautomation

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.StringRequest
import com.google.android.material.snackbar.Snackbar
import com.mysofttechnology.homeautomation.adapters.RoomsRecyclerAdapter
import com.mysofttechnology.homeautomation.databinding.FragmentRoomsBinding
import com.mysofttechnology.homeautomation.models.RoomsViewModel
import com.mysofttechnology.homeautomation.utils.VolleySingleton
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

private const val TAG = "RoomsFragment"

class RoomsFragment : Fragment() {

    private lateinit var roomAdapter: RoomsRecyclerAdapter
    private lateinit var roomTouchHelper: ItemTouchHelper

    private lateinit var loadingDialog: LoadingDialog
    private var currentUserId: String? = null
    private var sharedPref: SharedPreferences? = null

    private var _binding: FragmentRoomsBinding? = null
    private val bind get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRoomsBinding.inflate(inflater, container, false)
        bind.roomRecyclerview.layoutManager = LinearLayoutManager(requireActivity())
        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingDialog = LoadingDialog()
        loadingDialog.isCancelable = false

        sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE) ?: return
        currentUserId = sharedPref!!.getString(getString(R.string.current_user_id), "")


        roomAdapter = RoomsRecyclerAdapter(requireActivity(), roomsData())

//        if (roomsData().isEmpty()) {
//            bind.msg.visibility = View.VISIBLE
//        } else bind.msg.visibility = View.GONE

        bind.roomsBackBtn.setOnClickListener {
            bind.roomsBackBtn.isEnabled = false
            Navigation.findNavController(it).navigate(R.id.action_roomsFragment_to_dashbordFragment)
        }

        bind.addRoomFab.setOnClickListener {
            bind.addRoomFab.isEnabled = false
            Navigation.findNavController(it)
                .navigate(R.id.action_roomsFragment_to_scanDeviceFragment)
        }

        bind.roomRecyclerview.adapter = roomAdapter
        roomTouchHelper = ItemTouchHelper(roomItemTouchHelper)

        roomTouchHelper.attachToRecyclerView(bind.roomRecyclerview)
    }

    private fun roomsData(): MutableList<RoomsViewModel> {
        val roomsData = ArrayList<RoomsViewModel>()
        loadingDialog.show(childFragmentManager, "$TAG roomsData")
        val requestQueue = VolleySingleton.getInstance(requireContext()).requestQueue
        val url = getString(R.string.base_url) + getString(R.string.url_room_list)

        if (isOnline()) {
            val stringRequest = object : StringRequest(Method.POST, url,
                { response ->
                    try {
                        val mData = JSONObject(response.toString())
                        val resp = mData.get("response") as Int
                        val msg = mData.get("msg")

                        if (resp == 1) {
                            val roomListData = mData.get("data") as JSONArray
                            for (i in 0 until roomListData.length()) {
                                val device = roomListData.getJSONObject(i)
                                val deviceName = device.get("room_name").toString()
                                val deviceId = device.get("device_id").toString()
                                val id = device.get("ID").toString()
                                roomsData.add(RoomsViewModel(deviceName, deviceId, id))
                                roomAdapter.notifyDataSetChanged()
                            }
                            loadingDialog.dismiss()

                            Log.d(TAG, "checkDatabase: Message - $msg")
                        } else {
                            showLSnackbar("Couldn't find any room.")
                            loadingDialog.dismiss()
                            Log.d(TAG, "checkDatabase: Message - $msg")
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
        return roomsData
    }

    private var roomItemTouchHelper = object :
        ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.START or ItemTouchHelper.END or ItemTouchHelper.DOWN or ItemTouchHelper.UP,
            0
        ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {

            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition

            Collections.swap(roomsData(), fromPosition, toPosition)

            bind.roomRecyclerview.adapter?.notifyItemMoved(fromPosition, toPosition)

            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    }

    private fun showToast(message: String?) {
        Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show()
    }

    private fun showLSnackbar(msg: String = "Something went wrong.") {
        if (context != null) {
            Snackbar.make(bind.roomsRootView, msg, Snackbar.LENGTH_INDEFINITE)
                .setAction("Retry") {
                    if (isOnline()) roomsData()
                    else showLSnackbar(msg)
                }
                .show()
        } else {
            Log.e(TAG, "showLSnackbar: Contect Error - $context")
            roomsData()
        }
    }

    private fun isOnline(): Boolean {
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
        return false
    }
}


// TODO: Use this code for fragments
//
//
//private var _binding: FragmentNameBinding? = null
//private val bind get() = _binding!!
//
//override fun onCreateView(
//    inflater: LayoutInflater, container: ViewGroup?,
//    savedInstanceState: Bundle?
//): View? {
//    _binding = FragmentNameBinding.inflate(inflater, container, false)
//    return bind.root
//}
//
//override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//    super.onViewCreated(view, savedInstanceState)
//
//    bind.backBtn.setOnClickListener {
//        Navigation.findNavController(it).navigate(R.id.action)
//    }
//}